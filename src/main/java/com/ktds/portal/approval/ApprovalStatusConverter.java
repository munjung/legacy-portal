package com.ktds.portal.approval;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * [리팩토링] ApprovalStatus enum ↔ DB 정수 컬럼 매핑.
 * DB 에는 기존과 동일하게 0/1/2/3/9 정수가 저장된다 — 마이그레이션 불필요, 기존 데이터와 호환.
 */
@Converter
public class ApprovalStatusConverter implements AttributeConverter<ApprovalStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ApprovalStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public ApprovalStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : ApprovalStatus.fromCode(code);
    }
}
