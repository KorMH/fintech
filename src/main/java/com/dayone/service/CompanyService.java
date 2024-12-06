package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRepository;
import com.dayone.persist.entity.CompanyEntity;
import com.dayone.persist.entity.DividendEntity;
import com.dayone.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CompanyService {

    private final Trie trie;
    private final CompanyRepository companyRepository;
    private final Scraper scraper;
    private final DividendRepository dividendRepository;

    public Company save(String ticker) {
        boolean exists = this.companyRepository.existsByTicker(ticker);
        if(exists){
            throw new RuntimeException("already exists ticker ->" + ticker);
        }

        try{
            return this.storeCompanyAndDividend(ticker);
        }catch (MappingException e) {
            throw new NotYetImplementedException();
        }
    }

    public Page<CompanyEntity> getAllCompany(Pageable pageable) {
        return this.companyRepository.findAll(pageable);
    }

    private Company storeCompanyAndDividend(String ticker) {
        // 1. ticker 를 기준으로 회사를 스크래핑
        Company company = scraper.scrapCompanyByTicker(ticker);
        // 2. 해당 회사가 존재할 경우, 회사의 배당금 정보를 스크래핑
        if (ObjectUtils.isEmpty(company)){
            throw new NoCompanyException();
        }
        ScrapedResult scrap = scraper.scrap(company);
        // 3. 스크래핑 결과 반환
        CompanyEntity savedEntity = this.companyRepository.save(new CompanyEntity(company));
        List<DividendEntity> dividendEntities = scrap.getDividends().stream().map(
                e-> new DividendEntity(savedEntity.getId(),e)
        ).toList();

        dividendRepository.saveAll(dividendEntities);
        try{
            return company;
        } catch (MappingException e){
            throw new NotYetImplementedException();
        }
    }

    public List<String> getCompanyNamesByKeyword(String keyword) {
        throw new NotYetImplementedException();
    }

    public void addAutocompleteKeyword(String keyword) {
        this.trie.put(keyword, null);
    }

    public List<String> autocomplete(String keyword) {
        return (List<String>) this.trie.prefixMap(keyword).keySet()
                .stream()
                .collect(Collectors.toList());
    }

    public void deleteAutocompleteKeyword(String keyword) {
        this.trie.remove(keyword);
    }

    public String deleteCompany(String ticker) {
        // 1. 배당금 정보 삭제
        CompanyEntity companyEntity = companyRepository.findByTicker(ticker).orElseThrow(
                () -> new NoCompanyException()
        );
        dividendRepository.deleteAllByCompanyId(companyEntity.getId());
        // 2. 회사 정보 삭제
        companyRepository.deleteById(companyEntity.getId());
        try{
            return companyEntity.getName();
        } catch (MappingException e){
            throw new NotYetImplementedException();
        }
    }

}
