package com.softwaremind.invoicedocbackend.invoice.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.invoice.InvoiceEntity;
import com.softwaremind.invoicedocbackend.invoice.InvoiceItemEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final CryptoService cryptoService;

    public byte[] generateInvoicePdf(InvoiceEntity invoice) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);

            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("VAT Invoice " + invoice.getNumber(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(20f);

            // ===== SELLER =====
            PdfPCell sellerCell = new PdfPCell();
            sellerCell.setBorder(Rectangle.NO_BORDER);
            sellerCell.addElement(new Paragraph("Seller:", boldFont(12)));
            sellerCell.addElement(new Paragraph(invoice.getSellerName()));

            AddressEmbeddable sellerAddr = invoice.getSellerAddress();
            if (sellerAddr != null) {
                sellerCell.addElement(new Paragraph(formatAddress(sellerAddr)));
            }

            String sellerNip = decryptOrNull(invoice.getSellerNipEncrypted());
            if (isPresent(sellerNip)) {
                sellerCell.addElement(new Paragraph("NIP: " + sellerNip));
            }

            SellerProfileEntity sellerProfile = invoice.getSellerProfile();
            if (sellerProfile != null) {
                String sellerRegon = sellerProfile.getRegon();
                if (isPresent(sellerRegon)) {
                    sellerCell.addElement(new Paragraph("REGON: " + sellerRegon));
                }

                String sellerKrs = sellerProfile.getKrs();
                if (isPresent(sellerKrs)) {
                    sellerCell.addElement(new Paragraph("KRS: " + sellerKrs));
                }
            }

            // bank name + account
            String bankName = sellerProfile != null ? sellerProfile.getBankName() : null;
            String bankAccount = invoice.getSellerBankAccount();
            if (!isPresent(bankAccount) && sellerProfile != null) {
                // fallback – np. jeśli w invoice nie zapisaliśmy snapshotu
                bankAccount = sellerProfile.getBankAccount();
            }

            if (isPresent(bankName) || isPresent(bankAccount)) {
                if (isPresent(bankName)) {
                    sellerCell.addElement(new Paragraph("Bank: " + bankName));
                }
                if (isPresent(bankAccount)) {
                    sellerCell.addElement(new Paragraph("Account:\n" + bankAccount));
                }
            }

            // ===== BUYER =====
            PdfPCell buyerCell = new PdfPCell();
            buyerCell.setBorder(Rectangle.NO_BORDER);
            buyerCell.addElement(new Paragraph("Buyer:", boldFont(12)));
            buyerCell.addElement(new Paragraph(invoice.getBuyerName()));

            AddressEmbeddable buyerAddr = invoice.getBuyerAddress();
            if (buyerAddr != null) {
                buyerCell.addElement(new Paragraph(formatAddress(buyerAddr)));
            }

            String buyerNip = decryptOrNull(invoice.getBuyerNipEncrypted());
            if (isPresent(buyerNip)) {
                buyerCell.addElement(new Paragraph("NIP: " + buyerNip));
            }

            ContractorEntity contractor = invoice.getContractor();
            if (contractor != null) {
                String buyerEmail = contractor.getEmail();
                if (isPresent(buyerEmail)) {
                    buyerCell.addElement(new Paragraph("Email: " + buyerEmail));
                }

                String buyerPhone = contractor.getPhone();
                if (isPresent(buyerPhone)) {
                    buyerCell.addElement(new Paragraph("Phone: " + buyerPhone));
                }
            }

            headerTable.addCell(sellerCell);
            headerTable.addCell(buyerCell);
            document.add(headerTable);

            // ===== DATES =====
            Paragraph dates = new Paragraph();
            dates.add(new Phrase("Issue date: " + invoice.getIssueDate() + "\n"));
            dates.add(new Phrase("Sale date: " + invoice.getSaleDate() + "\n"));
            dates.add(new Phrase("Due date: " + invoice.getDueDate() + "\n"));
            dates.setSpacingAfter(15f);
            document.add(dates);

            // ===== ITEMS TABLE =====
            PdfPTable table = new PdfPTable(new float[]{4f, 1f, 2f, 2f, 2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            addHeaderCell(table, "Description");
            addHeaderCell(table, "Qty");
            addHeaderCell(table, "Net amount");
            addHeaderCell(table, "VAT rate");
            addHeaderCell(table, "Gross amount");

            for (InvoiceItemEntity item : invoice.getItems()) {
                table.addCell(normalCell(item.getDescription()));
                table.addCell(rightCell(item.getQuantity().toPlainString()));
                table.addCell(rightCell(formatMoney(item.getNetTotal())));

                String vatLabel = item.getVatRate();
                if (vatLabel != null && vatLabel.matches("\\d+(\\.\\d+)?")) {
                    vatLabel = vatLabel + "%";
                }
                table.addCell(rightCell(vatLabel != null ? vatLabel : ""));
                table.addCell(rightCell(formatMoney(item.getGrossTotal())));
            }

            document.add(table);

            // ===== SUMMARY =====
            PdfPTable summary = new PdfPTable(2);
            summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summary.setWidthPercentage(40);
            summary.setSpacingBefore(10f);

            summary.addCell(borderlessCell(""));
            summary.addCell(borderlessRightCell(""));

            summary.addCell(borderlessCell("Net total:"));
            summary.addCell(borderlessRightCell(formatMoney(invoice.getTotalNet())));

            summary.addCell(borderlessCell("VAT total:"));
            summary.addCell(borderlessRightCell(formatMoney(invoice.getTotalVat())));

            summary.addCell(borderlessCell("Gross total:"));
            summary.addCell(borderlessRightCell(
                    formatMoney(invoice.getTotalGross()) + " " + invoice.getCurrency()
            ));

            document.add(summary);

            // ===== NOTES =====
            if (isPresent(invoice.getNotes())) {
                Paragraph notes = new Paragraph("\nNotes:\n" + invoice.getNotes());
                notes.setLeading(14f);
                document.add(notes);
            }

            // ===== SIGNATURES =====
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(40f);

            String dots = ".".repeat(30);

            PdfPCell sellerSignCell = borderlessCell(null);
            sellerSignCell.setHorizontalAlignment(Element.ALIGN_LEFT);

            Paragraph sellerDots = new Paragraph(dots);
            sellerDots.setAlignment(Element.ALIGN_CENTER);

            Paragraph sellerLabel = new Paragraph("(Seller)");
            sellerLabel.setAlignment(Element.ALIGN_CENTER);

            sellerSignCell.addElement(sellerDots);
            sellerSignCell.addElement(sellerLabel);

            PdfPCell buyerSignCell = borderlessCell(null);
            buyerSignCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph buyerDots = new Paragraph(dots);
            buyerDots.setAlignment(Element.ALIGN_CENTER);

            Paragraph buyerLabel = new Paragraph("(Buyer)");
            buyerLabel.setAlignment(Element.ALIGN_CENTER);

            buyerSignCell.addElement(buyerDots);
            buyerSignCell.addElement(buyerLabel);

            signTable.addCell(sellerSignCell);
            signTable.addCell(buyerSignCell);

            document.add(signTable);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate invoice PDF", e);
        }
    }

    private String decryptOrNull(String enc) {
        return enc != null ? cryptoService.decrypt(enc) : null;
    }

    private boolean isPresent(String value) {
        return value != null
                && !value.isBlank()
                && !"null".equalsIgnoreCase(value);
    }

    private String formatAddress(AddressEmbeddable addr) {
        if (addr == null) return "";

        StringBuilder sb = new StringBuilder();

        if (addr.getStreet() != null) {
            sb.append(addr.getStreet());
        }
        if (addr.getBuildingNumber() != null && !addr.getBuildingNumber().isBlank()) {
            sb.append(" ").append(addr.getBuildingNumber());
        }
        if (addr.getApartmentNumber() != null && !addr.getApartmentNumber().isBlank()) {
            sb.append("/").append(addr.getApartmentNumber());
        }
        sb.append("\n");

        if (addr.getPostalCode() != null) {
            sb.append(addr.getPostalCode()).append(" ");
        }
        if (addr.getCity() != null) {
            sb.append(addr.getCity());
        }

        if (addr.getCountry() != null && !addr.getCountry().isBlank()) {
            sb.append("\n").append(addr.getCountry());
        }

        return sb.toString();
    }

    private String formatMoney(BigDecimal val) {
        return val.setScale(2).toPlainString();
    }

    private Font boldFont(float size) {
        return new Font(Font.HELVETICA, size, Font.BOLD);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, boldFont(10)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private PdfPCell normalCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : ""));
        cell.setPadding(4f);
        return cell;
    }

    private PdfPCell rightCell(String text) {
        PdfPCell cell = normalCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell borderlessCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : ""));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3f);
        return cell;
    }

    private PdfPCell borderlessRightCell(String text) {
        PdfPCell cell = borderlessCell(text);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}
