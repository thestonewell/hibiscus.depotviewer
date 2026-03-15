package de.open4me.depot.gui.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import de.open4me.depot.Settings;
import de.open4me.depot.sql.GenericObjectSQL;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

public class OrderExportAction implements Action
{
	@Override
	public void handleAction(Object context) throws ApplicationException
	{
		GenericObjectSQL[] entries = toArray(context);
		if (entries == null || entries.length == 0) {
			throw new ApplicationException("Bitte mindestens einen Eintrag auswählen.");
		}

		File file = chooseFile();
		if (file == null) {
			return;
		}

		export(entries, file);
		Application.getMessagingFactory().sendMessage(new StatusBarMessage(
				entries.length + " Einträge exportiert nach " + file.getName(),
				StatusBarMessage.TYPE_SUCCESS));
	}

	private GenericObjectSQL[] toArray(Object context) throws ApplicationException {
		if (context instanceof GenericObjectSQL[]) {
			return (GenericObjectSQL[]) context;
		}
		if (context instanceof GenericObjectSQL) {
			return new GenericObjectSQL[] {(GenericObjectSQL) context};
		}
		throw new ApplicationException("Bitte mindestens einen Eintrag auswählen.");
	}

	private File chooseFile() {
		FileDialog dialog = new FileDialog(GUI.getShell(), SWT.SAVE);
		dialog.setText("Bitte wählen Sie die CSV-Datei für den Export aus:");
		dialog.setFilterExtensions(new String[] {"*.csv", "*.*"});
		dialog.setFilterNames(new String[] {"CSV-Dateien", "Alle Dateien"});
		dialog.setFileName("orderbuch-export.csv");
		String filename = dialog.open();
		if (filename == null || filename.trim().isEmpty()) {
			return null;
		}
		if (!filename.toLowerCase().endsWith(".csv")) {
			filename += ".csv";
		}
		return new File(filename);
	}

	private void export(GenericObjectSQL[] entries, File file) throws ApplicationException {
		CSVFormat format = CSVFormat.EXCEL.withDelimiter(';');
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
				CSVPrinter printer = new CSVPrinter(writer, format)) {
			printer.printRecord("Depot", "WKN", "Wertpapiername", "Anzahl", "Kurs", "Kurswährung",
					"Kosten", "Kostenwährung", "Gebühren", "Gebührenwährung",
					"Steuern", "Steuernwährung", "Aktion", "Datum", "Kommentar");
			for (GenericObjectSQL entry : entries) {
				printer.printRecord(
						value(entry, "bezeichnung"),
						value(entry, "wkn"),
						value(entry, "wertpapiername"),
						value(entry, "anzahl"),
						value(entry, "kurs"),
						value(entry, "kursw"),
						value(entry, "kosten"),
						value(entry, "kostenw"),
						value(entry, "transaktionskosten"),
						value(entry, "transaktionskostenw"),
						value(entry, "steuern"),
						value(entry, "steuernw"),
						value(entry, "aktion"),
						value(entry, "buchungsdatum"),
						value(entry, "kommentar"));
			}
		} catch (Exception e) {
			throw new ApplicationException("Fehler beim CSV-Export", e);
		}
	}

	private String value(GenericObjectSQL entry, String attribute) throws Exception {
		Object value = entry.getAttribute(attribute);
		if (value == null) {
			return "";
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toPlainString();
		}
		if (value instanceof Date) {
			return Settings.DATEFORMAT.format((Date) value);
		}
		return value.toString();
	}
}
