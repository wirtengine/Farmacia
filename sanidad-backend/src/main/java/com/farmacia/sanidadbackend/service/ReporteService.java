package com.farmacia.sanidadbackend.service;

import com.farmacia.sanidadbackend.model.Venta;
import com.farmacia.sanidadbackend.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final VentaRepository ventaRepository;

    public String generarExcelVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws IOException {

        // 🔥 CORREGIDO AQUÍ
        List<Venta> ventas = ventaRepository.findByFechaBetweenAndActivoTrue(fechaInicio, fechaFin);

        String fileName = "reporte_ventas_" + UUID.randomUUID() + ".xlsx";
        String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ventas");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"ID", "Factura", "Fecha", "Cliente", "Total", "Vendedor"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Venta v : ventas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(v.getId());
                row.createCell(1).setCellValue(v.getNumeroFactura());
                row.createCell(2).setCellValue(v.getFecha().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                row.createCell(3).setCellValue(v.getCliente() != null ? v.getCliente().getNombre() : "Consumidor final");
                row.createCell(4).setCellValue(v.getTotal().doubleValue());
                row.createCell(5).setCellValue(v.getUsuario().getUsername());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }

        return filePath;
    }

    public String generarPdfVentas(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws IOException {

        // 🔥 CORREGIDO AQUÍ TAMBIÉN
        List<Venta> ventas = ventaRepository.findByFechaBetweenAndActivoTrue(fechaInicio, fechaFin);

        String fileName = "reporte_ventas_" + UUID.randomUUID() + ".pdf";
        String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Reporte de Ventas").setFontSize(18).setBold());

            document.add(new Paragraph("Período: " +
                    fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE) + " a " +
                    fechaFin.format(DateTimeFormatter.ISO_LOCAL_DATE)));

            Table table = new Table(5);
            table.addCell("Factura");
            table.addCell("Fecha");
            table.addCell("Cliente");
            table.addCell("Total");
            table.addCell("Vendedor");

            for (Venta v : ventas) {
                table.addCell(v.getNumeroFactura());
                table.addCell(v.getFecha().format(DateTimeFormatter.ISO_LOCAL_DATE));
                table.addCell(v.getCliente() != null ? v.getCliente().getNombre() : "Consumidor final");
                table.addCell(v.getTotal().toString());
                table.addCell(v.getUsuario().getUsername());
            }

            document.add(table);
        }

        return filePath;
    }
}