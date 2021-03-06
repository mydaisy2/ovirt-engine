package org.ovirt.engine.ui.common.widget.table.column;

import org.ovirt.engine.ui.common.widget.renderer.DiskSizeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.SizeConverter;

public abstract class DiskSizeColumn<T> extends RenderedTextColumn<T, Long> {

    public DiskSizeColumn() {
        this(SizeConverter.SizeUnit.BYTES);
    }

    public DiskSizeColumn(SizeConverter.SizeUnit diskSizeUnit) {
        super(new DiskSizeRenderer<Long>(diskSizeUnit));
    }

    public DiskSizeColumn(SizeConverter.SizeUnit diskSizeUnit, DiskSizeRenderer.Format format) {
        super(new DiskSizeRenderer<Long>(diskSizeUnit, format));
    }
}
