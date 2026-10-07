package com.ena.mam.cassettecut;

import com.ena.mam.cassettecategory.CassetteCategoryRepository;
import com.ena.mam.digitizedmedia.DigitizedMediaRepository;
import com.ena.mam.staffmember.StaffMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class CassetteCutService {
    private final CassetteCutRepository cassetteCutRepository;
    private final CassetteCutMapper cassetteCutMapper;
    private final CassetteCategoryRepository cassetteCategoryRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final DigitizedMediaRepository digitizedMediaRepository;

    public CassetteCutService(CassetteCutRepository cassetteCutRepository, CassetteCutMapper cassetteCutMapper, CassetteCategoryRepository cassetteCategoryRepository, StaffMemberRepository staffMemberRepository, DigitizedMediaRepository digitizedMediaRepository) {
        this.cassetteCutRepository = cassetteCutRepository;
        this.cassetteCutMapper = cassetteCutMapper;
        this.cassetteCategoryRepository = cassetteCategoryRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.digitizedMediaRepository = digitizedMediaRepository;
    }

}
