package com.odilias.odms.util;

import com.odilias.odms.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PrintUtil {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final double PAGE_WIDTH = 550;
    private static final double LEFT_PADDING = 40;
    private static final double RIGHT_PADDING = 40;
    private static final double CONTENT_WIDTH = PAGE_WIDTH - LEFT_PADDING - RIGHT_PADDING;

    public static void printInvoice(Stage owner, Billing billing, Patient patient) {
        VBox receipt = createReceiptNode(billing, patient, new ArrayList<>());
        executePrint(owner, receipt, "Document sent to printer/PDF.");
    }

    public static void printInvoiceWithHistory(Stage owner, Billing billing, Patient patient, List<Payment> history) {
        VBox receipt = createReceiptNode(billing, patient, history);
        executePrint(owner, receipt, "Invoice/Receipt sent to printer/PDF.");
    }

    public static void printMedicalRecord(Stage owner, MedicalRecord record, Patient patient) {
        VBox note = createMedicalRecordNode(record, patient);
        executePrint(owner, note, "Medical Record sent to printer/PDF.");
    }

    public static void printLabResult(Stage owner, LabResult result, Patient patient) {
        VBox report = createLabResultNode(result, patient);
        executePrint(owner, report, "Lab Report sent to printer/PDF.");
    }

    public static void printPatientProfile(Stage owner, Patient patient) {
        VBox profile = createPatientProfileNode(patient);
        executePrint(owner, profile, "Patient Profile sent to printer/PDF.");
    }

    public static void printAppointmentSlip(Stage owner, Appointment appt) {
        VBox slip = createAppointmentSlipNode(appt);
        executePrint(owner, slip, "Appointment Slip sent to printer/PDF.");
    }

    private static void executePrint(Stage owner, VBox node, String successMsg) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(owner)) {
            boolean success = job.printPage(node);
            if (success) {
                job.endJob();
                ToastUtil.show(owner, successMsg, ToastUtil.ToastType.SUCCESS);
            }
        }
    }

    private static VBox createBaseContainer() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(40, LEFT_PADDING, 40, RIGHT_PADDING));
        container.setStyle("-fx-background-color: white;");
        container.setPrefWidth(PAGE_WIDTH);
        return container;
    }

    private static VBox createHeader(String title) {
        VBox header = new VBox(5);
        header.setAlignment(Pos.CENTER);
        Label hospitalName = new Label("ODILIAS DISPENSARY");
        hospitalName.setFont(Font.font("System", FontWeight.BOLD, 24));
        hospitalName.setTextFill(Color.web("#2ecc71"));
        Label slogan = new Label("Professional Healthcare Through Automation");
        slogan.setFont(Font.font("System", 12));
        Label docType = new Label(title);
        docType.setFont(Font.font("System", FontWeight.BOLD, 18));
        docType.setPadding(new Insets(10, 0, 0, 0));
        header.getChildren().addAll(hospitalName, slogan, docType);
        return header;
    }

    private static VBox createFooter() {
        Label footer = new Label(
                "Thank you for choosing Odilias Dispensary\nComputer Generated - No Signature Required");
        footer.setAlignment(Pos.CENTER);
        footer.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        footer.setPadding(new Insets(40, 0, 0, 0));
        footer.setPrefWidth(CONTENT_WIDTH);
        footer.setFont(Font.font("System", 10));
        return new VBox(footer);
    }

    private static GridPane createPatientInfoGrid(Patient patient, LocalDateTime date) {
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(50);
        infoGrid.setVgap(5);
        infoGrid.add(new Label("Patient Name:"), 0, 0);
        Label pName = new Label(patient.getFullName());
        pName.setFont(Font.font("System", FontWeight.BOLD, 14));
        infoGrid.add(pName, 0, 1);
        infoGrid.add(new Label("Patient ID: " + patient.getPatientUuid()), 0, 2);
        infoGrid.add(new Label("Date: " + date.format(dtf)), 1, 0);
        return infoGrid;
    }

    private static VBox createReceiptNode(Billing billing, Patient patient, List<Payment> history) {
        VBox container = createBaseContainer();
        boolean isPaid = billing.getPaidAmount().compareTo(billing.getTotalAmount()) >= 0;
        container.getChildren().add(createHeader(isPaid ? "OFFICIAL RECEIPT" : "INVOICE"));
        container.getChildren().add(new Separator());

        GridPane info = createPatientInfoGrid(patient,
                billing.getBillingDate() != null ? billing.getBillingDate() : LocalDateTime.now());
        info.add(new Label("Payment: " + (billing.getPaymentMethod() != null ? billing.getPaymentMethod() : "PENDING")),
                1, 1);
        info.add(new Label("Status: " + billing.getPaymentStatus()), 1, 2);
        container.getChildren().add(info);

        // ITEMS TABLE
        VBox table = new VBox(5);
        table.setPadding(new Insets(20, 0, 10, 0));
        HBox th = new HBox(10);
        th.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8;");
        Label dCol = new Label("Description");
        dCol.setPrefWidth(240);
        dCol.setStyle("-fx-font-weight: bold;");
        Label qCol = new Label("Qty");
        qCol.setPrefWidth(40);
        qCol.setStyle("-fx-font-weight: bold;");
        Label pCol = new Label("Price");
        pCol.setPrefWidth(80);
        pCol.setStyle("-fx-font-weight: bold;");
        Label sCol = new Label("Total");
        sCol.setPrefWidth(110);
        sCol.setAlignment(Pos.CENTER_RIGHT);
        sCol.setStyle("-fx-font-weight: bold;");
        th.getChildren().addAll(dCol, qCol, pCol, sCol);
        table.getChildren().add(th);

        if (billing.getItems().isEmpty()) {
            Label noItems = new Label("No specific items listed.");
            noItems.setPadding(new Insets(10));
            table.getChildren().add(noItems);
        } else {
            for (InvoiceItem item : billing.getItems()) {
                HBox tr = new HBox(10);
                tr.setPadding(new Insets(5, 8, 5, 8));
                tr.getChildren().addAll(
                        new Label(item.getItemName()) {
                            {
                                setPrefWidth(240);
                                setWrapText(true);
                            }
                        },
                        new Label(String.valueOf(item.getQuantity())) {
                            {
                                setPrefWidth(40);
                            }
                        },
                        new Label(item.getUnitPrice().toString()) {
                            {
                                setPrefWidth(80);
                            }
                        },
                        new Label(item.getSubtotal().toString()) {
                            {
                                setPrefWidth(110);
                                setAlignment(Pos.CENTER_RIGHT);
                            }
                        });
                table.getChildren().add(tr);
            }
        }
        container.getChildren().add(table);

        VBox totals = new VBox(5);
        totals.setAlignment(Pos.CENTER_RIGHT);
        totals.setPadding(new Insets(10, 0, 0, 0));

        totals.getChildren().add(new Label("Subtotal:  KES " + formatCurrency(billing.getTotalAmount())));
        totals.getChildren().add(new Label("Paid Amount:  KES " + formatCurrency(billing.getPaidAmount())));

        Label grandTotal = new Label("Balance Due:  KES " + formatCurrency(billing.getBalance()));
        grandTotal.setFont(Font.font("System", FontWeight.BOLD, 18));
        totals.getChildren().add(grandTotal);

        HBox totalsWrapper = new HBox(totals);
        totalsWrapper.setAlignment(Pos.CENTER_RIGHT);
        container.getChildren().add(totalsWrapper);

        // PAYMENT HISTORY
        if (history != null && !history.isEmpty()) {
            VBox historyBox = new VBox(5);
            historyBox.setPadding(new Insets(20, 0, 0, 0));
            Label hTitle = new Label("PAYMENT HISTORY");
            hTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
            historyBox.getChildren().addAll(hTitle, new Separator());

            for (Payment p : history) {
                HBox hr = new HBox(20);
                hr.getChildren().addAll(
                        new Label(p.getPaymentDate().format(dtf)),
                        new Label(p.getPaymentMethod().toString()),
                        new Region() {
                            {
                                HBox.setHgrow(this, Priority.ALWAYS);
                            }
                        },
                        new Label("KES " + formatCurrency(p.getAmount())) {
                            {
                                setFont(Font.font("System", FontWeight.BOLD, 12));
                            }
                        });
                historyBox.getChildren().add(hr);
            }
            container.getChildren().add(historyBox);
        }

        container.getChildren().add(createFooter());
        return container;
    }

    private static VBox createMedicalRecordNode(MedicalRecord record, Patient patient) {
        VBox container = createBaseContainer();
        container.getChildren().add(createHeader("CLINICAL CONSULTATION NOTE"));
        container.getChildren().add(new Separator());

        GridPane info = createPatientInfoGrid(patient, record.getRecordDate());
        info.add(new Label("Clinician: " + record.getClinicianName()), 1, 1);
        container.getChildren().add(info);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 0, 20, 0));

        // Vitals
        HBox vitals = new HBox(20);
        vitals.setStyle("-fx-background-color: #f1f2f6; -fx-padding: 10; -fx-background-radius: 5;");
        vitals.getChildren().addAll(
                new Label("BP: " + (record.getBloodPressure() != null ? record.getBloodPressure() : "N/A")),
                new Label("Temp: " + (record.getTemperature() != null ? record.getTemperature() + "°C" : "N/A")),
                new Label("Pulse: " + (record.getPulseRate() != null ? record.getPulseRate() + " bpm" : "N/A")),
                new Label("Weight: " + (record.getWeight() != null ? record.getWeight() + " kg" : "N/A")));
        content.getChildren().add(vitals);

        content.getChildren().add(createSection("DIAGNOSIS", record.getDiagnosis()));
        content.getChildren().add(createSection("TREATMENT PLAN", record.getTreatment()));
        content.getChildren().add(createSection("PRESCRIPTIONS", record.getPrescriptions()));
        content.getChildren().add(createSection("LABORATORY", record.getLabResults()));

        container.getChildren().add(content);
        container.getChildren().add(createFooter());
        return container;
    }

    private static VBox createLabResultNode(LabResult result, Patient patient) {
        VBox container = createBaseContainer();
        container.getChildren().add(createHeader("LABORATORY REPORT"));
        container.getChildren().add(new Separator());

        GridPane info = createPatientInfoGrid(patient,
                result.getCompletedAt() != null ? result.getCompletedAt() : LocalDateTime.now());
        info.add(new Label("Requested By: " + result.getClinicianName()), 1, 1);
        container.getChildren().add(info);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 0, 20, 0));

        Label testName = new Label(result.getTestName());
        testName.setFont(Font.font("System", FontWeight.BOLD, 16));
        testName.setStyle("-fx-background-color: #e8f4fd; -fx-padding: 10;");
        testName.setPrefWidth(CONTENT_WIDTH);
        content.getChildren().add(testName);

        GridPane resultGrid = new GridPane();
        resultGrid.setHgap(30);
        resultGrid.setVgap(10);
        resultGrid.setPadding(new Insets(10));
        resultGrid.add(new Label("PARAMETER"), 0, 0);
        resultGrid.add(new Label("FINDING"), 1, 0);
        resultGrid.add(new Label("REFERENCE RANGE"), 2, 0);

        Label rLabel = new Label(result.getResultData());
        rLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        resultGrid.add(new Label(result.getTestName()), 0, 1);
        resultGrid.add(rLabel, 1, 1);
        resultGrid.add(new Label(recordValue(result.getReferenceRange())), 2, 1);
        content.getChildren().add(resultGrid);

        content.getChildren().add(createSection("REMARKS / COMMENTS", result.getComments()));

        container.getChildren().add(content);
        container.getChildren().add(createFooter());
        return container;
    }

    private static VBox createPatientProfileNode(Patient patient) {
        VBox container = createBaseContainer();
        container.getChildren().add(createHeader("PATIENT COMPREHENSIVE PROFILE"));
        container.getChildren().add(new Separator());

        GridPane info = createPatientInfoGrid(patient, LocalDateTime.now());
        container.getChildren().add(info);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20, 0, 20, 0));

        GridPane details = new GridPane();
        details.setHgap(30);
        details.setVgap(10);
        details.add(new Label("Gender:"), 0, 0);
        details.add(new Label(patient.getGender()), 1, 0);
        details.add(new Label("Age:"), 0, 1);
        details.add(new Label(String.valueOf(patient.getAge())), 1, 1);
        details.add(new Label("Contact:"), 0, 2);
        details.add(new Label(patient.getContactNumber()), 1, 2);
        details.add(new Label("Address:"), 0, 3);
        details.add(new Label(patient.getAddress()), 1, 3);
        details.add(new Label("Registered:"), 0, 4);
        details.add(new Label(patient.getCreatedAt() != null ? patient.getCreatedAt().toString() : "N/A"), 1, 4);

        content.getChildren().add(details);
        container.getChildren().add(content);
        container.getChildren().add(createFooter());
        return container;
    }

    private static VBox createAppointmentSlipNode(Appointment appt) {
        VBox container = createBaseContainer();
        container.getChildren().add(createHeader("APPOINTMENT CONFIRMATION SLIP"));
        container.getChildren().add(new Separator());

        GridPane info = new GridPane();
        info.setHgap(50);
        info.setVgap(10);
        info.add(new Label("Patient:"), 0, 0);
        info.add(new Label(appt.getPatientName()), 1, 0);
        info.add(new Label("Date:"), 0, 1);
        info.add(
                new Label(
                        appt.getAppointmentTime() != null ? appt.getAppointmentTime().toLocalDate().toString() : "N/A"),
                1, 1);
        info.add(new Label("Time:"), 0, 2);
        info.add(
                new Label(
                        appt.getAppointmentTime() != null ? appt.getAppointmentTime().toLocalTime().toString() : "N/A"),
                1, 2);
        info.add(new Label("Staff:"), 0, 3);
        info.add(new Label(appt.getStaffName()), 1, 3);
        info.add(new Label("Reason:"), 0, 4);
        info.add(new Label(appt.getReason()), 1, 4);
        container.getChildren().add(info);

        container.getChildren().add(createFooter());
        return container;
    }

    private static String recordValue(String v) {
        return (v == null || v.isEmpty()) ? "-" : v;
    }

    private static VBox createSection(String title, String content) {
        VBox section = new VBox(5);
        Label t = new Label(title);
        t.setFont(Font.font("System", FontWeight.BOLD, 12));
        t.setTextFill(Color.web("#7f8c8d"));
        Label c = new Label((content == null || content.isEmpty()) ? "None recorded" : content);
        c.setWrapText(true);
        c.setPrefWidth(CONTENT_WIDTH);
        section.getChildren().addAll(t, c);
        return section;
    }

    private static String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null)
            return "0.00";
        return String.format("%,.2f", amount);
    }
}
