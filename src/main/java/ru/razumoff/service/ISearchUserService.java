package ru.razumoff.service;

import ru.razumoff.dao.dto.internal.SearchUserDto;
import ru.razumoff.dao.dto.response.UserSearchRsDto;

import java.util.List;

public interface ISearchUserService {
    List<UserSearchRsDto> searchByFio(SearchUserDto request);
}
