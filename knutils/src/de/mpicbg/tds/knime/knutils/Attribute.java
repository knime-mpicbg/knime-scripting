package de.mpicbg.tds.knime.knutils;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import org.knime.core.data.*;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageBlobCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * @author Holger Brandl
 */
public class Attribute<AttributeType> {

    private final String attributeName;
    private final DataType attributeType;


    public Attribute(String attributeName, DataType type) {
        this.attributeName = attributeName;
        this.attributeType = type;
    }


    public DataType getType() {
        return attributeType;
    }


    public String getName() {
        return attributeName;
    }


    public DataColumnSpec getColumnSpec() {
        DataColumnSpecCreator columnSpecCreator = new DataColumnSpecCreator(getName(), getType());

        return columnSpecCreator.createSpec();
    }


    //
    //  cell generation
    //


    public DataCell createCell(DataRow dataRow) {
        return createCell(getValue(dataRow));
    }


    public DataCell createCell(Object value) {
        if (value == null) {
            return DataType.getMissingCell();
        }

        if (getType().equals(DoubleCell.TYPE)) {
            if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    return DataType.getMissingCell();
                }

                value = Double.parseDouble((String) value);
            }

            if (value instanceof Integer) {
                value = ((Integer) value).doubleValue();
            }

            return new DoubleCell((Double) value);

        } else if (getType().equals(IntCell.TYPE)) {
            if (value instanceof String) {
                if (((String) value).trim().isEmpty()) {
                    return DataType.getMissingCell();
                }

                value = Integer.parseInt((String) value);
            }

            return postProcessCreatedCell(new IntCell((Integer) value));

        } else if (getType().equals(StringCell.TYPE)) {

            return postProcessCreatedCell(new StringCell("" + value));

        } else if (getType().equals(DateAndTimeCell.TYPE)) {
            Date date = (Date) value;
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(date);

            return new DateAndTimeCell(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        } else {
            throw new RuntimeException("Unsupported attribute-type");
        }
    }


    protected DataCell postProcessCreatedCell(DataCell cell) {
        return cell;
    }


    //
    //  attribute value extraction
    //


    public String getNominalAttribute(DataRow dataRow) {
        if (!isNominalAttribute()) {
            throw new RuntimeException("Could not cast attribute value to string");
        }

        DataCell dataCell = dataRow.getCell(getColumnIndex());

        if (dataCell.isMissing()) {
            return null;
        }

        if (dataCell instanceof StringCell) {
            return ((StringCell) dataCell).getStringValue();
        } else {
            throw new RuntimeException("Could not extract value of cell with type " + dataCell);
        }
    }


    public Integer getIntAttribute(DataRow dataRow) {
        Double doubleAttribute = getDoubleAttribute(dataRow);

        return doubleAttribute != null ? doubleAttribute.intValue() : null;
    }


    public Double getDoubleAttribute(DataRow dataRow) {
        if (!isNumerical()) {
            throw new RuntimeException("Could not cast attribute " + this + " to double. Use the String2Number node to convert the type");
        }

        DataCell dataCell = dataRow.getCell(getColumnIndex());

        if (dataCell.isMissing()) {
            return null;

        } else if (dataCell instanceof IntCell) {
            return ((IntCell) dataCell).getDoubleValue();

        } else if (dataCell instanceof DoubleCell) {
            return ((DoubleCell) dataCell).getDoubleValue();

        } else {
            throw new RuntimeException("type of cell not yet mappd: " + dataCell);
        }
    }


    public Date getDateAttribute(DataRow dataRow) {
        if (!isDateAttribute()) {
            throw new RuntimeException("Could not cast attribute value to date");
        }

        DataCell cell = dataRow.getCell(getColumnIndex());
        if (cell.isMissing()) {
            return null;
        }

        DateAndTimeCell dateCell = (DateAndTimeCell) cell;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return dateFormat.parse(dateCell.getDayOfMonth() + "." + (dateCell.getMonth() + 1) + "." + dateCell.getYear());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public PNGImageCell getImageAttribute(DataRow dataRow) {
        if ( !isImageAttribute() ) {
            throw new RuntimeException(("Could not cast attribute to Image value."));
        }

        DataCell cell = dataRow.getCell(getColumnIndex());
        if ( cell.isMissing() ) {
            return null;
        }

        PNGImageCell pngCell = (PNGImageCell) cell;
        return pngCell;
    }


    public boolean isNumerical() {
        return isDoubleType() || isIntegerType();
    }

    public boolean isDoubleType() {
        return getType().equals(DoubleCell.TYPE);
    }

    public boolean isIntegerType() {
        return getType().equals(IntCell.TYPE);
    }


    public boolean isDateAttribute() {
        return getColumnSpec().getType().equals(DateAndTimeCell.TYPE);
    }


    public boolean isMissing(DataRow dataRow) {
        return dataRow.getCell(getColumnIndex()).isMissing();
    }


    public boolean isNominalAttribute() {
        // todo that's the proper way to do it; we should adapt this everywhere!
//        return getColumnSpec().getType().getValueClasses().contains(StringValue.class);

        return getColumnSpec().getType().equals(StringCell.TYPE);
    }


    public boolean isImageAttribute() {
        return getColumnSpec().getType().equals(PNGImageContent.TYPE);
        // TODO check for third party types. KNIP
    }


    public AttributeType getValue(DataRow dataRow) {
        if (isNominalAttribute()) {
            return (AttributeType) getNominalAttribute(dataRow);
        } else if (isDateAttribute()) {
            return (AttributeType) getDateAttribute(dataRow);
        } else if (isIntegerType()) {
            Integer intAttribute = getIntAttribute(dataRow);
            return intAttribute != null ? (AttributeType) new Integer(intAttribute) : null;
        } else if (isNumerical()) {
            Double doubleAttribute = getDoubleAttribute(dataRow);
            return doubleAttribute != null ? (AttributeType) new Double(doubleAttribute) : null;
        }

        throw new RuntimeException("Not supported column type");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attribute)) return false;

        Attribute attribute = (Attribute) o;

        if (!getName().equals(attribute.getName())) return false;

        return true;
    }


    @Override
    public int hashCode() {
        return getName().hashCode();
    }


    @Override
    public String toString() {
        String type = null;

        if (isNumerical()) {
            type = "numerical";
        } else if (isNominalAttribute()) {
            type = "nominal";
        } else if (isDateAttribute()) {
            type = "date";
        }

        return getName() + " (" + type + ")";
    }


    public String getRawValue(DataRow dataRow) {
        if (isNominalAttribute()) {
            return getNominalAttribute(dataRow);
        }

        return dataRow.getCell(getColumnIndex()).toString();
    }


    public Integer getColumnIndex() {
        throw new RuntimeException("not implemented for generic attributes. Use InputTableAttribute for reading from input tables");
    }
}
