package com.farmacia.service;

import com.farmacia.model.Devolucion;
import com.farmacia.model.Venta;
import com.farmacia.model.VentaDetalle;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class DevolucionPdfService {

    private static final Logger log = LoggerFactory.getLogger(DevolucionPdfService.class);

    public byte[] generarPdfDevolucion(Devolucion devolucion) throws IOException, WriterException {
        log.info("Generando PDF para devolución ID: {}, esTotal: {}", devolucion.getId(), devolucion.getDetalle() == null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // Título
        document.add(new Paragraph("COMPROBANTE DE DEVOLUCIÓN")
                .setBold()
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));

        // Datos de la devolución
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("N° de Devolución: " + devolucion.getId()));
        document.add(new Paragraph("Fecha de solicitud: " + devolucion.getFechaSolicitud().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        if (devolucion.getFechaAprobacion() != null) {
            document.add(new Paragraph("Fecha de aprobación: " + devolucion.getFechaAprobacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        }
        document.add(new Paragraph("Estado: " + devolucion.getEstado()));
        document.add(new Paragraph("Motivo: " + devolucion.getMotivo()));
        if (devolucion.getObservacionAdmin() != null && !devolucion.getObservacionAdmin().isEmpty()) {
            document.add(new Paragraph("Observación: " + devolucion.getObservacionAdmin()));
        }
        document.add(new Paragraph("\n"));

        // Datos de la venta original
        Venta venta = devolucion.getVenta();
        document.add(new Paragraph("VENTA ORIGINAL").setBold().setFontSize(14));
        document.add(new Paragraph("Factura N°: " + venta.getNumeroFactura()));
        document.add(new Paragraph("Fecha de venta: " + venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        if (venta.getCliente() != null) {
            document.add(new Paragraph("Cliente: " + venta.getCliente().getNombreCompleto()));
        }
        document.add(new Paragraph("Vendedor: " + venta.getVendedor().getNombre() + " " + venta.getVendedor().getApellido()));
        document.add(new Paragraph("\n"));

        // Productos devueltos
        document.add(new Paragraph("PRODUCTOS DEVUELTOS").setBold().setFontSize(14));

        if (devolucion.getDetalle() != null) {
            // Devolución parcial
            VentaDetalle detalle = devolucion.getDetalle();
            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Producto");
            table.addHeaderCell("Cantidad");
            table.addHeaderCell("Precio Unit.");
            table.addHeaderCell("Subtotal");
            table.addCell(detalle.getMedicamento().getNombre());
            table.addCell(String.valueOf(devolucion.getCantidad()));
            table.addCell("C$ " + detalle.getPrecioUnitario());
            table.addCell("C$ " + detalle.getPrecioUnitario().multiply(new java.math.BigDecimal(devolucion.getCantidad())));
            document.add(table);
        } else {
            // Devolución total
            if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
                document.add(new Paragraph("No hay productos asociados a esta venta."));
                log.warn("La venta ID {} no tiene detalles al generar PDF de devolución total", venta.getId());
            } else {
                Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}));
                table.setWidth(UnitValue.createPercentValue(100));
                table.addHeaderCell("Producto");
                table.addHeaderCell("Cantidad");
                table.addHeaderCell("Precio Unit.");
                table.addHeaderCell("Subtotal");
                for (VentaDetalle d : venta.getDetalles()) {
                    table.addCell(d.getMedicamento().getNombre());
                    table.addCell(String.valueOf(d.getCantidad()));
                    table.addCell("C$ " + d.getPrecioUnitario());
                    table.addCell("C$ " + d.getSubtotal());
                }
                document.add(table);
            }
        }

        document.add(new Paragraph("\n"));

        // Código QR con enlace al PDF
        String baseUrl = "http://localhost:8080"; // Cambiar en producción
        String qrContent = baseUrl + "/api/devoluciones/" + devolucion.getId() + "/pdf";

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
        Image qrImage = new Image(ImageDataFactory.create(qrBaos.toByteArray()));
        qrImage.setWidth(100);
        qrImage.setHeight(100);
        document.add(qrImage);
        document.add(new Paragraph("Escanee para descargar el comprobante"));

        document.close();
        log.info("PDF generado exitosamente para devolución ID: {}", devolucion.getId());
        return baos.toByteArray();
    }
}