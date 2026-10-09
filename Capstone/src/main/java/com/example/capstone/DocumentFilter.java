package com.example.capstone;

public record DocumentFilter(
        String docId,
        String section,
        String equipment,
        String revision,
        String effectiveDate) {

    public boolean isEmpty() {
        return (docId == null || docId.isBlank())
                && (section == null || section.isBlank())
                && (equipment == null || equipment.isBlank())
                && (revision == null || revision.isBlank())
                && (effectiveDate == null || effectiveDate.isBlank());
    }

    public static DocumentFilter empty() {
        return new DocumentFilter(null, null, null, null, null);
    }
}
