package de.open4me.depot.gui.control;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.open4me.depot.Settings;
import de.open4me.depot.gui.DatumsSlider;
import de.open4me.depot.gui.action.OrderList;
import de.open4me.depot.gui.menu.BestandsListMenu;
import de.open4me.depot.gui.parts.PrintfColumn;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.tools.Bestandsabfragen;
import de.willuhn.jameica.gui.AbstractControl;
import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Part;
import de.willuhn.jameica.gui.formatter.DateFormatter;
import de.willuhn.jameica.gui.parts.TablePart;
import de.willuhn.logging.Logger;


public class BestandTableControl extends AbstractControl implements Listener
{

	private TablePart bestandsList;
	private DatumsSlider datumsSlider;
	private Date currentdate;

	public BestandTableControl(AbstractView view, DatumsSlider datumsSlider)
	{
		super(view);
		currentdate = null;
		this.datumsSlider = datumsSlider;
		datumsSlider.addListener(this);
	}

	public void showForDate(Date d) throws RemoteException {
		try {
			List<GenericObjectSQL> list = Bestandsabfragen.getBestand(d);
			bestandsList.removeAll();
			for (GenericObjectSQL x : list) {
				bestandsList.addItem(x);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@SuppressWarnings("rawtypes")
	public Part getBestandsTabelle() throws Exception
	{
		if (bestandsList != null) {
			return bestandsList;
		}

		bestandsList = new TablePart(Bestandsabfragen.getBestand(null),new OrderList()) {

			@SuppressWarnings("unchecked")
			@Override
			protected String getSummary() {
				try {

					double sum = 0.0d;
					for (GenericObjectSQL k: (List<GenericObjectSQL>) getItems())
					{
						if (k.getAttribute("wert") == null) {
							continue;
						}
						if (k.getAttribute("wert") instanceof BigDecimal) {
							sum += ((BigDecimal) k.getAttribute("wert")).doubleValue();
						} else {
							sum += (Double) k.getAttribute("wert");

						}
					}

					return String.format("Gesamt-Saldo: %.2f", sum);
				}
				catch (Exception e)
				{
					Logger.error("Kann Gesamt-Saldo nicht berechnen",e);
				}
				return super.getSummary();
			}

		};

		bestandsList.setRememberColWidths(true);
		bestandsList.setRememberOrder(true);
		bestandsList.addColumn(Settings.i18n().tr("Depot"), "bezeichnung");
		bestandsList.addColumn(Settings.i18n().tr("WKN"),"wkn");
		bestandsList.addColumn(Settings.i18n().tr("Name"),"wertpapiername");
		bestandsList.addColumn(new PrintfColumn(Settings.i18n().tr("Anzahl"), "anzahl",	"%,.5f", "anzahl"));
		bestandsList.addColumn(new PrintfColumn(Settings.i18n().tr("Kurs"), "kurs", "%,.6f %s", "kurs", "kursw"));
		bestandsList.addColumn(new PrintfColumn(Settings.i18n().tr("Wert"), "wert", "%,.2f %s", "wert", "wertw"));
		bestandsList.addColumn(Settings.i18n().tr("Bewertungsdatum"),"bewertungszeitpunkt", new DateFormatter(Settings.DATEFORMAT));
		bestandsList.addColumn(Settings.i18n().tr("Abrufdatum"),"datum", new DateFormatter(Settings.DATEFORMAT));
		bestandsList.setContextMenu(new BestandsListMenu(bestandsList));
		showForDate(null);
		return bestandsList;
	}

	@Override
	public void handleEvent(Event event) {
		if (currentdate != datumsSlider.getDate()) {
			currentdate = datumsSlider.getDate();
			try {
				showForDate(currentdate);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
