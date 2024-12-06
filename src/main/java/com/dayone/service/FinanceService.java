package com.dayone.service;

import com.dayone.exception.impl.NoCompanyException;
import com.dayone.model.Company;
import com.dayone.model.Dividend;
import com.dayone.model.ScrapedResult;
import com.dayone.model.constants.CacheKey;
import com.dayone.persist.CompanyRepository;
import com.dayone.persist.DividendRepository;
import com.dayone.persist.entity.CompanyEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {
    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;


    @Cacheable(key = "#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName) {
        // 1. 회사명을 기준으로 회사 정보를 조회
        CompanyEntity company = companyRepository.findByName(companyName).orElseThrow(
                () -> new NoCompanyException()
        );
        // 2. 조회된 회사 ID 로 배당금 정보 조회
        ScrapedResult scrapedResult = new ScrapedResult();
        scrapedResult.setCompany(
                Company.builder()
                        .ticker(company.getTicker())
                        .name(company.getName())
                        .build()
        );
        // 3. 결과 조합 후 반환
        List<Dividend> list = dividendRepository.findAllByCompanyId(company.getId()).stream().map(
                a->{
                    Dividend div = new Dividend();
                    div.setDividend(a.getDividend());
                    div.setDate(a.getDate());
                    return div;
                }).collect(Collectors.toList());
        scrapedResult.setDividends(list);
        try{
            return scrapedResult;
        } catch (MappingException e) {
            throw new NotYetImplementedException();
        }
    }
}
