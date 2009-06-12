package com.faktel.features;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.faktel.InvoiceRow;
import com.faktel.InvoiceRows;
import com.faktel.Utils;
import com.faktel.filters.FilterArgs;
import com.faktel.filters.RowFilter;
import com.faktel.gui.FakGUI;
import com.faktel.mvc.Grid;
import com.faktel.mvc.GridRow;
import com.faktel.mvc.Model;
import com.faktel.mvc.View;

public class BreakInvoiceIntoSingleNumberInvoices implements RowFilter {
	private String m_outputViewName;
	
	public static final String POOL_DATA_SMALL_INVOICES = BreakInvoiceIntoSingleNumberInvoices.class
			.getName()
			+ ".smallInvoices HashMap<String, InvoiceRows>";

	public BreakInvoiceIntoSingleNumberInvoices(FilterArgs args) {
		if (args.get("outputView") != null && args.get("outputView").size() >= 1) {
			m_outputViewName = args.get("outputView").iterator().next();
		}
	}

	public boolean cleanup(Model model, File workingDir, File commonDir) {
		return true;
	}

	public Grid execute(Model model, File workingDir, File allFiltersDir) {
		InvoiceRows invoice = model.getRows();
		HashMap<String, InvoiceRows> allNumbers = new HashMap<String, InvoiceRows>();

		int invoicesCounter = 0;
		int rowsConuter = 0;
		Grid res = new Grid();
		for (InvoiceRow row : invoice) {
			// assuming tel number is unique (and sim is not)
			String key = row.getTelNumber();
			InvoiceRows oneNumberInvoice = allNumbers.get(key);
			if (null == oneNumberInvoice) {
				// not present yet
				assert (!allNumbers.containsKey(key));
				oneNumberInvoice = new InvoiceRows();
				allNumbers.put(key, oneNumberInvoice);
				invoicesCounter++;
			}

			oneNumberInvoice.add(row);
			rowsConuter++;
		}

		res.add(new GridRow(new Object[] { "Invoice split into "
				+ invoicesCounter + " single-number invoices " + rowsConuter
				+ " rows in total.",
				"Number of entries",
				""}));

		// dump small invoice sizes to output
		Set<Entry<String, InvoiceRows>> entries = allNumbers.entrySet();
		for (Entry<String, InvoiceRows> entry : entries) {
			String key = entry.getKey();
			InvoiceRows value = entry.getValue();
			int size = value.size();

			res.add(new GridRow(new Object[] {
					new String("Invoice for " + key + " has "),
					new String("" + size), new String(" entries.") }));
			// dump all small invoices to XLS files

			String fileName = workingDir.getAbsolutePath() + File.separator
					+ key + ".xls";
			String sheetName = "Invoice";
			Utils.outputFilterResultToXls(value.toGrid(), fileName, sheetName);
		}

		model.getDataPool().put(
				BreakInvoiceIntoSingleNumberInvoices.POOL_DATA_SMALL_INVOICES,
				allNumbers);
		
		if (m_outputViewName != null && FakGUI.getApplication() != null) {
			View outputView = FakGUI.getApplication().getView(m_outputViewName);
			
			if (outputView != null) {
				outputView.displayGrid(res);
			}
		}
		
		return res;
	}

	public boolean prepare(FilterArgs args) {
		return true;
	}

}
