package org.ovirt.engine.ui.common.widget.table.resize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.widget.table.column.EmptyColumn;
import org.ovirt.engine.ui.common.widget.table.column.SafeHtmlCellWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.GridController;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;

/**
 * A {@link CellTable} that supports resizing its columns using mouse.
 * <p>
 * Column resize feature is disabled by default, use {@link #enableColumnResizing} to enable it.
 *
 * @param <T>
 *            Table row data type.
 */
public class ColumnResizeCellTable<T> extends CellTable<T> implements HasResizableColumns<T> {

    private static final int DEFAULT_MINIMUM_COLUMN_WIDTH = 30;

    private int minimumColumnWidth = DEFAULT_MINIMUM_COLUMN_WIDTH;

    // Prefix for keys used to store widths of individual columns
    private static final String GRID_COLUMN_WIDTH_PREFIX = "GridColumnWidth"; //$NON-NLS-1$


    // Empty, no-width column used with resizable columns feature
    // that occupies remaining horizontal space within the table
    private Column<T, ?> emptyNoWidthColumn;

    private boolean columnResizingEnabled = false;
    private boolean columnResizePersistenceEnabled = false;

    // used to store column width preferences
    private ClientStorage clientStorage;

    // used to store column width preferences
    private GridController gridController;

    // used to prevent default column widths from overriding persisted column widths
    private final List<Column<T, ?>> initializedColumns = new ArrayList<Column<T, ?>>();

    public ColumnResizeCellTable() {
        super();
    }

    public ColumnResizeCellTable(int pageSize, ProvidesKey<T> keyProvider) {
        super(pageSize, keyProvider);
    }

    public ColumnResizeCellTable(int pageSize, CellTable.Resources resources,
            ProvidesKey<T> keyProvider, Widget loadingIndicator) {
        super(pageSize, resources, keyProvider, loadingIndicator);
    }

    public ColumnResizeCellTable(int pageSize, CellTable.Resources resources,
            ProvidesKey<T> keyProvider) {
        super(pageSize, resources, keyProvider);
    }

    public ColumnResizeCellTable(int pageSize, CellTable.Resources resources) {
        super(pageSize, resources);
    }

    public ColumnResizeCellTable(int pageSize) {
        super(pageSize);
    }

    public ColumnResizeCellTable(ProvidesKey<T> keyProvider) {
        super(keyProvider);
    }

    /**
     * {@inheritDoc}
     * <p>
     * When calling this method, consider using a header that supports displaying tooltip in case the header content
     * doesn't fit the header element.
     */
    @Override
    public void addColumn(Column<T, ?> column, Header<?> header) {
        super.addColumn(column, header);

        if (columnResizingEnabled) {
            if (emptyNoWidthColumn != null) {
                removeColumn(emptyNoWidthColumn);
            }

            // Add empty, no-width column as the last column
            emptyNoWidthColumn = new EmptyColumn<T>();
            addColumn(emptyNoWidthColumn);
        }
    }

    /**
     * Adds a new column, without specifying column width.
     */
    @Override
    public void addColumn(Column<T, ?> column, String headerText) {
        addColumn(column, createHeader(column, headerText, false));
    }

    /**
     * Adds a new column with SafeHtml header text, without specifying column width.
     */
    @Override
    public void addColumn(Column<T, ?> column, SafeHtml headerHtml) {
        addColumn(column, createHeader(column, headerHtml));
    }

    /**
     * Adds a new column, setting the column width.
     */
    public void addColumnAndSetWidth(Column<T, ?> column, String headerText, String width) {
        addColumn(column, headerText);
        setColumnWidth(column, width);
    }

    /**
     * Adds a new column with SafeHtml header text, setting the column width.
     */
    public void addColumnAndSetWidth(Column<T, ?> column, SafeHtml headerHtml, String width) {
        addColumn(column, headerHtml);
        setColumnWidth(column, width);
    }

    /**
     * Adds a new column with HTML header text, without specifying column width.
     * <p>
     * {@code headerHtml} must honor the SafeHtml contract as specified in
     * {@link com.google.gwt.safehtml.shared.SafeHtmlUtils#fromSafeConstant(String) SafeHtmlUtils.fromSafeConstant}.
     */
    public void addColumnWithHtmlHeader(Column<T, ?> column, String headerHtml) {
        addColumn(column, createHeader(column, headerHtml, true));
    }

    /**
     * Adds a new column with HTML header text, setting the column width.
     * <p>
     * {@code headerHtml} must honor the SafeHtml contract as specified in
     * {@link com.google.gwt.safehtml.shared.SafeHtmlUtils#fromSafeConstant(String) SafeHtmlUtils.fromSafeConstant}.
     */
    public void addColumnWithHtmlHeader(Column<T, ?> column, String headerHtml, String width) {
        addColumnWithHtmlHeader(column, headerHtml);
        setColumnWidth(column, width);
    }

    /**
     * Removes a column.
     *
     */
    @Override
    public void removeColumn(int index) {
        Column<T, ?> column = getColumn(index);
        super.removeColumn(index);
        initializedColumns.remove(column);
    }

    Header<?> createHeader(Column<T, ?> column, String headerTextOrHtml, boolean allowHtml) {
        SafeHtml headerHtml = allowHtml ? SafeHtmlUtils.fromSafeConstant(headerTextOrHtml)
                : SafeHtmlUtils.fromString(headerTextOrHtml);
        return createHeader(column, headerHtml);
    }

    Header<?> createHeader(Column<T, ?> column, SafeHtml headerHtml) {
        return columnResizingEnabled ? new ResizableHeader<T>(headerHtml, column, this)
                : createSafeHtmlHeader(headerHtml);
    }

    Header<?> createSafeHtmlHeader(final SafeHtml text) {
        return new Header<SafeHtml>(new SafeHtmlCellWithTooltip()) {
            @Override
            public SafeHtml getValue() {
                return text;
            }
        };
    }

    /**
     * Ensures that the given column is added (or removed), unless it's already present (or absent).
     */
    public void ensureColumnPresent(Column<T, ?> column, String headerText, boolean present) {
        ensureColumnPresent(column, headerText, present, null);
    }

    /**
     * Ensures that the given column is added (or removed), unless it's already present (or absent).
     * <p>
     * This method also sets the column width in case the column needs to be added.
     */
    public void ensureColumnPresent(Column<T, ?> column, String headerText, boolean present, String width) {
        if (present) {
            // Remove the column first
            if (getColumnIndex(column) != -1) {
                removeColumn(column);
            }

            // Re-add the column
            if (width == null) {
                addColumnWithHtmlHeader(column, headerText);
            } else {
                addColumnWithHtmlHeader(column, headerText, width);
            }
        } else if (!present && getColumnIndex(column) != -1) {
            // Remove the column
            removeColumn(column);
        }
    }

    @Override
    public void setColumnWidth(Column<T, ?> column, String width) {
        if (columnResizePersistenceEnabled && !initializedColumns.contains(column)) {
            String persistedWidth = readColumnWidth(column);
            if (persistedWidth != null) {
                width = persistedWidth;
            }
            initializedColumns.add(column);
        }

        super.setColumnWidth(column, width);

        if (columnResizingEnabled) {
            int columnIndex = getColumnIndex(column);
            TableElement tableElement = getElement().cast();

            // Update body and header cell widths
            for (TableCellElement cell : getTableBodyCells(tableElement, columnIndex)) {
                cell.getStyle().setProperty("width", width); //$NON-NLS-1$
            }
            for (TableCellElement cell : getTableHeaderCells(tableElement, columnIndex)) {
                cell.getStyle().setProperty("width", width); //$NON-NLS-1$
            }
        }
    }

    List<TableCellElement> getTableBodyCells(TableElement tableElement, int columnIndex) {
        TableSectionElement firstTBodyElement = tableElement.getTBodies().getItem(0);
        return firstTBodyElement != null ? getCells(firstTBodyElement.getRows(), columnIndex)
                : Collections.<TableCellElement> emptyList();
    }

    List<TableCellElement> getTableHeaderCells(TableElement tableElement, int columnIndex) {
        TableSectionElement tHeadElement = tableElement.getTHead();
        return tHeadElement != null ? getCells(tHeadElement.getRows(), columnIndex)
                : Collections.<TableCellElement> emptyList();
    }

    List<TableCellElement> getCells(NodeList<TableRowElement> rows, int columnIndex) {
        List<TableCellElement> result = new ArrayList<TableCellElement>();
        for (int i = 0; i < rows.getLength(); i++) {
            TableCellElement cell = rows.getItem(i).getCells().getItem(columnIndex);
            if (cell != null) {
                result.add(cell);
            }
        }
        return result;
    }

    /**
     * Allows table columns to be resized by dragging their right-hand border using mouse.
     * <p>
     * This method should be called before calling any {@code addColumn} methods.
     * <p>
     * <em>After calling this method, each column must have an explicit width defined in PX units, otherwise the resize
     * behavior will not function properly.</em>
     */
    public void enableColumnResizing() {
        columnResizingEnabled = true;

        // Column resize implementation needs table-layout:fixed (disable browser-specific table layout algorithm)
        setWidth("100%", true); //$NON-NLS-1$
    }

    @Override
    public void onResizeStart(Column<T, ?> column, Element headerElement) {
        headerElement.getStyle().setBackgroundColor("#D6DCFF"); //$NON-NLS-1$
    }

    @Override
    public void onResizeEnd(Column<T, ?> column, Element headerElement) {
        headerElement.getStyle().clearBackgroundColor();

        // Redraw the table
        redraw();

        if (columnResizePersistenceEnabled) {
            String width = getColumnWidth(column);
            saveColumnWidth(column, width);
        }
    }

    @Override
    public void resizeColumn(Column<T, ?> column, int newWidth) {
        setColumnWidth(column, newWidth + "px"); //$NON-NLS-1$
    }

    @Override
    public int getMinimumColumnWidth(Column<T, ?> column) {
        return minimumColumnWidth;
    }

    public void setMinimumColumnWidth(int minimumColumnWidth) {
        this.minimumColumnWidth = minimumColumnWidth;
    }

    /**
     * Enables saving this table's column widths to LocalStorage (or a cookie if LocalStorage unsupported).
     * @param clientStorage
     * @param gridController
     */
    public void enableColumnWidthPersistence(ClientStorage clientStorage, GridController gridController) {
        this.clientStorage = clientStorage;
        this.gridController = gridController;
        if (clientStorage != null && gridController != null) {
            columnResizePersistenceEnabled = true;
        }
    }

    protected String getColumnWidthKey(Column<T, ?> column) {
        if (columnResizePersistenceEnabled) {
            return GRID_COLUMN_WIDTH_PREFIX + "_" + gridController.getId() + "_" + getColumnIndex(column); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return null;
    }

    protected void saveColumnWidth(Column<T, ?> column, String width) {
        if (columnResizePersistenceEnabled) {
            String key = getColumnWidthKey(column);
            if (key != null) {
                clientStorage.setLocalItem(key, width);
            }
        }
    }

    protected String readColumnWidth(Column<T, ?> column) {
        if (columnResizePersistenceEnabled) {
            String key = getColumnWidthKey(column);
            if (key != null) {
                return clientStorage.getLocalItem(key);
            }
        }
        return null;
    }

}
