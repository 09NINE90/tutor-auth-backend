package ru.razumoff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.razumoff.dao.dto.internal.SearchUserDto;
import ru.razumoff.dao.dto.response.UserSearchRsDto;
import ru.razumoff.dao.repository.UserProfileRepository;
import ru.razumoff.minio.IMinioFileService;
import ru.razumoff.service.ISearchUserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchUserService implements ISearchUserService {

    private final UserProfileRepository repository;
    private final IMinioFileService minIOFileService;

    private static final String SEARCH_USER_REGEX = "[^a-zа-я0-9.@_-]";
    private static final String SPACE_PLUS = "\\s+";
    private static final String LIKE_PREFIX = "%";
    private static final String LIKE_POSTFIX = "%";
    private static final String EMPTY_STRING = "";

    /**
     * Поиск пользователей по ФИО с триграммным поиском
     * Возвращает аватарки с MinIO URL
     */
    @Override
    public List<UserSearchRsDto> searchByFio(SearchUserDto request) {
        if (request.getSearchQuery() == null) return Collections.emptyList();
        if (request.getSearchQuery().trim().length() < 2) return Collections.emptyList();

        List<String> trigrams = generateTrigramPatterns(request.getSearchQuery());

        if (trigrams.isEmpty()) return Collections.emptyList();
        String[] trigramArray = trigrams.toArray(new String[0]);

        List<UserSearchRsDto> users = repository.searchByTrigramPatterns(
                trigramArray,
                request.getSearchQuery(),
                request.getLimit(),
                request.getUserId(),
                request.getCourseId()
        );

        if (users.isEmpty()) return Collections.emptyList();

        for (UserSearchRsDto user : users) {
            String s3Key = user.getAvatarS3Key();
            user.setAvatarS3Key(minIOFileService.generatePublicUrl(s3Key));
        }
        return users;
    }

    /**
     * Генерация LIKE паттернов триграмм (%abc%)
     */
    private List<String> generateTrigramPatterns(String query) {
        List<String> trigrams = generateTrigrams(query);
        return trigrams.stream()
                .map(trigram -> LIKE_PREFIX + trigram + LIKE_POSTFIX)
                .collect(Collectors.toList());
    }

    /**
     * Разбиение запроса на триграммы (abc→a,ab,bc,b,c)
     */
    private List<String> generateTrigrams(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.toLowerCase().trim();

        String[] words = lowerQuery.split(SPACE_PLUS);

        List<String> allTrigrams = new ArrayList<>();

        for (String word : words) {
            String cleanWord = word.replaceAll(SEARCH_USER_REGEX, EMPTY_STRING);

            if (cleanWord.length() < 3) {
                continue;
            }

            List<String> wordTrigrams = IntStream.range(0, cleanWord.length() - 2)
                    .mapToObj(i -> cleanWord.substring(i, i + 3))
                    .toList();
            allTrigrams.addAll(wordTrigrams);
        }

        return allTrigrams.stream().distinct().toList();
    }

}
