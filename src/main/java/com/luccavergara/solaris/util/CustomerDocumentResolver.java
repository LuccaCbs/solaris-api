package com.luccavergara.solaris.util;

import com.luccavergara.solaris.entity.Customer;
import com.luccavergara.solaris.entity.CustomerDocument;
import com.luccavergara.solaris.entity.DocumentType;

import java.util.Comparator;
import java.util.List;

public final class CustomerDocumentResolver {

    private CustomerDocumentResolver() {
    }

    public static CustomerDocument resolveFiscalDocument(Customer customer) {
        List<CustomerDocument> documents = customer.getDocuments();

        if (documents == null || documents.isEmpty()) {
            return CustomerDocument.builder()
                    .documentType(customer.getDocumentType())
                    .documentNumber(customer.getDocumentNumber())
                    .build();
        }

        return documents.stream()
                .filter(document -> Boolean.TRUE.equals(document.getPrimary()))
                .findFirst()
                .or(() -> documents.stream()
                        .filter(document -> document.getDocumentType() == DocumentType.CUIT)
                        .findFirst())
                .or(() -> documents.stream()
                        .sorted(Comparator.comparing(CustomerDocument::getId))
                        .findFirst())
                .orElseThrow();
    }
}
