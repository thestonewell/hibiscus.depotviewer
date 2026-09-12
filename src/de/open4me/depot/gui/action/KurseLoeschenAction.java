package de.open4me.depot.gui.action;

import java.sql.Connection;
import java.sql.PreparedStatement;

import de.open4me.depot.abruf.utils.Utils;
import de.open4me.depot.gui.dialogs.Sicherheitsabfrage;
import de.open4me.depot.messaging.KursUpdatesMsg;
import de.open4me.depot.sql.GenericObjectSQL;
import de.open4me.depot.sql.SQLUtils;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;

/** Löscht alle Kurse und Kursereignisse der ausgewählten Wertpapiere. */
public class KurseLoeschenAction implements Action
{
    @Override
    public void handleAction(Object context) throws ApplicationException
    {
        GenericObjectSQL[] wertpapiere = getWertpapiere(context);
        if (wertpapiere.length == 0)
            return;

        try
        {
            if (!new Sicherheitsabfrage(getDeleteMessage(wertpapiere)).open())
                return;

            deleteKurse(wertpapiere);
            Utils.markRecalc(null);
            Application.getMessagingFactory().sendMessage(new KursUpdatesMsg(wertpapiere[0].getID()));
        }
        catch (Exception e)
        {
            throw new ApplicationException("Fehler beim Löschen der Kurse.", e);
        }
    }

    private static GenericObjectSQL[] getWertpapiere(Object context)
    {
        if (context instanceof GenericObjectSQL[])
            return (GenericObjectSQL[]) context;
        if (context instanceof GenericObjectSQL)
            return new GenericObjectSQL[] { (GenericObjectSQL) context };
        return new GenericObjectSQL[0];
    }

    private static String getDeleteMessage(GenericObjectSQL[] wertpapiere)
    {
        if (wertpapiere.length == 1)
            return "Wollen Sie wirklich alle Kurse und Kursereignisse des ausgewählten Wertpapiers löschen?";
        return "Wollen Sie wirklich alle Kurse und Kursereignisse der " + wertpapiere.length
                + " ausgewählten Wertpapiere löschen?";
    }

    private static void deleteKurse(GenericObjectSQL[] wertpapiere) throws Exception
    {
        try (Connection conn = SQLUtils.getConnection())
        {
            conn.setAutoCommit(false);
            try
            {
                deleteFrom(conn, "depotviewer_kursevent", wertpapiere);
                deleteFrom(conn, "depotviewer_kurse", wertpapiere);
                conn.commit();
            }
            catch (Exception e)
            {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void deleteFrom(Connection conn, String table, GenericObjectSQL[] wertpapiere) throws Exception
    {
        try (PreparedStatement delete = conn.prepareStatement("delete from " + table + " where wpid = ?"))
        {
            for (GenericObjectSQL wertpapier : wertpapiere)
            {
                delete.setString(1, wertpapier.getID());
                delete.addBatch();
            }
            delete.executeBatch();
        }
    }
}
