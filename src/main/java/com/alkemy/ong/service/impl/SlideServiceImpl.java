package com.alkemy.ong.service.impl;

import com.alkemy.ong.dto.slide.SlideBasicResponseDto;

import com.alkemy.ong.dto.slide.SlideRequestDto;
import com.alkemy.ong.exception.BadRequestException;
import com.alkemy.ong.exception.EmptyListException;
import com.alkemy.ong.exception.NotFoundException;
import com.alkemy.ong.dto.slide.SlideResponseDto;
import com.alkemy.ong.mapper.GenericMapper;
import com.alkemy.ong.exception.UnableToDeleteEntityException;
import com.alkemy.ong.exception.UnableToUpdateEntityException;
import com.alkemy.ong.model.Organization;
import com.alkemy.ong.model.Slide;
import com.alkemy.ong.service.IAmazonClient;
import com.alkemy.ong.service.ISlideService;
import com.alkemy.ong.repository.OrganizationRepository;
import com.alkemy.ong.repository.SlideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
public class SlideServiceImpl implements ISlideService {


    private final SlideRepository slideRepository;
    private final OrganizationRepository organizationRepository;
    private final GenericMapper mapper;
    private final MessageSource messageSource;
    private final IAmazonClient amazonClient;

    public SlideResponseDto create(SlideRequestDto dto) throws Exception {

        Organization org = organizationRepository.findAll().get(0);
        Slide slide = mapper.map(dto, Slide.class);
        slide.setOrganizationId(org.getId());
        List<Slide> slidesList = slideRepository.findAll();
        int n = 0;
        if (dto.getPosition() == null) {
            slide.setPosition(slidesList.size() + 1);
            slidesList.add(slide);
            n++;
        } else if (slidesList.isEmpty() || (slidesList.size() < dto.getPosition())) {
            slide.setPosition(slidesList.size() + 1);
            slidesList.add(slide);
        } else if (!slidesList.isEmpty() && (slidesList.size() >= dto.getPosition())) {
            slide.setPosition(dto.getPosition());
            slidesList.add(dto.getPosition(), slide);
        }

        slide.setImage(amazonClient.uploadFile(slide.getImage(), slide.getText()));

        slide = slideRepository.save(slide);
        SlideResponseDto responseDTO = mapper.map(slide, SlideResponseDto.class);
        if (n == 1)
            responseDTO.setMessage(messageSource.getMessage("slide-position", null, Locale.US));
        return responseDTO;
    }

    @Override
    public List<SlideBasicResponseDto> getAll() {
        List<Slide> slides = slideRepository.findAllByOrderByPositionAsc();
        if (slides.isEmpty())
            throw new EmptyListException(messageSource.getMessage("empty-list", null, Locale.US));
        return mapper.mapAll(slides, SlideBasicResponseDto.class);
    }

    public SlideResponseDto getById(Long id) {
        if (id < 1) {
            throw new BadRequestException(messageSource.getMessage("error-negative-id", null, Locale.US));
        }
        Slide slide = slideRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messageSource.getMessage("slide-not-found", null, Locale.US)));

        return mapper.map(slide, SlideResponseDto.class);
    }

    public List<SlideResponseDto> findByOrganizationId(Long organizationId){
        List<SlideResponseDto> slides = slideRepository.findByOrganizationId(organizationId);

        if (slides.isEmpty()) {
            throw new EmptyListException(messageSource.getMessage
                    ("empty-list", null, Locale.US));
        }
        slides.sort(Comparator.comparing(SlideResponseDto::getPosition));
        return slides;
    }

    public SlideResponseDto update(SlideRequestDto dto, Long id) {
        Slide entity = getSlideById(id);
        try {
            Slide updatedEntity = mapper.map(dto, Slide.class);
            updatedEntity.setId(entity.getId());
            updatedEntity.setOrganizationId(entity.getOrganizationId());
            updatedEntity.setCreationDate(entity.getCreationDate());
            updatedEntity.setUpdateDate(LocalDateTime.now());
            updatedEntity = slideRepository.save(updatedEntity);
            return mapper.map(updatedEntity, SlideResponseDto.class);
        } catch (Exception e) {
            throw new UnableToUpdateEntityException(messageSource.getMessage("unable-to-update-slide", new Object[] {id}, Locale.US));
        }
    }

    @Override
    public void delete(Long id){
        Optional<Slide> exists = slideRepository.findById(id);
        if (exists.isEmpty()){
            throw new NotFoundException(messageSource.getMessage("slide-not-found",null, Locale.US));
        }
        try {
            Slide slide = exists.get();
            slide.setUpdateDate(LocalDateTime.now());
            slideRepository.delete(slide);
        }catch (Exception e){
            throw new UnableToDeleteEntityException(messageSource.getMessage("unable-to-delete-slide",new Object[] {id}, Locale.US));
        }
    }

    private Slide getSlideById(Long id) {
        Optional<Slide> entity = slideRepository.findById(id);
        if (entity.isEmpty())
            throw new NotFoundException(messageSource.getMessage("slide-not-found", new Object[] {id} ,Locale.US));
        return entity.get();
    }

}
