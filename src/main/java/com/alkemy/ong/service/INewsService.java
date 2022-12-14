package com.alkemy.ong.service;

import com.alkemy.ong.dto.PageDto;
import com.alkemy.ong.dto.news.NewsRequestDto;
import com.alkemy.ong.dto.news.NewsResponseDto;

public interface INewsService {

    NewsResponseDto getById(Long id);
    PageDto<NewsResponseDto> getPage(int pageNum);
    NewsResponseDto create(NewsRequestDto dto);
    void delete(Long id);
    NewsResponseDto update(NewsRequestDto news, Long id);

}
