package net.sf.JRecord.Details;

import net.sf.JRecord.Common.Constants;
import net.sf.JRecord.Common.Conversion;
import net.sf.JRecord.Common.FieldDetail;
import net.sf.JRecord.Common.IFieldDetail;
import net.sf.JRecord.Common.RecordException;
import net.sf.JRecord.CsvParser.ICsvLineParser;
import net.sf.JRecord.CsvParser.CsvDefinition;
import net.sf.JRecord.CsvParser.ParserManager;
import net.sf.JRecord.Types.Type;
import net.sf.JRecord.Types.TypeChar;
import net.sf.JRecord.Types.TypeManager;

public class CharLine extends BasicLine implements AbstractLine {

	private static LineProvider defaultProvider = new CharLineProvider();

	private String data;

	public CharLine(LayoutDetail layoutDef, String line) {
		super(defaultProvider, layoutDef);

		if (line == null) {
			data = "";
		} else {
			data = line;
		}
	}


	@Override
	public byte[] getData(int start, int len) {
		String tmpData = data;
	    int tempLen = Math.min(len, tmpData.length() - start + 1);

	    if ((tmpData.length() < start) || (tempLen < 1) || (start < 1)) {
	        return NULL_RECORD;
	    }
	    //System.out.println();
	    //System.out.println("getData --> " + " " + start + ", " + len + ", " + tempLen + " ~ " + tmpData.length()
	    // 		+ " " + tmpData);
		return Conversion.getBytes(
				tmpData.substring(
						start - 1,
						start + tempLen - 1
				),
				layout.getFontName());
	}


	@Override
	public byte[] getData() {
		return Conversion.getBytes(data, layout.getFontName());
	}



	@SuppressWarnings("deprecation")
	@Override
	public Object getField(int typeId, IFieldDetail field) {

		if (field.isFixedFormat()) {
			if (typeId == Type.ftChar
			||  typeId == Type.ftCharRightJust
			||  typeId == Type.ftCharRestOfRecord 
			||  TypeManager.getInstance().getType(typeId) == TypeManager.getInstance().getType(Type.ftChar)) {
				return getFieldText(field);
			}

			Type type = TypeManager.getSystemTypeManager().getType(typeId);

            int pos =  field.calculateActualPosition(this);
            
			byte[] bytes = getData(pos, field.getLen());
			FieldDetail tmpField = new FieldDetail(field.getFontName(), field.getDescription(),
					typeId, field.getDecimal() ,field.getFontName(), field.getFormat(), field.getParamater());
			tmpField.setPosLen(1, bytes.length);

			return type.getField(bytes, 1, tmpField);
		} else {
			return layout.formatCsvField(field, typeId, data.toString());
		}
	}



	@Override
	public byte[] getFieldBytes(int recordIdx, int fieldIdx) {
		return null;
	}


	@Override
	public String getFieldText(int recordIdx, int fieldIdx) {
		return  getFieldText(layout.getRecord(recordIdx).getField(fieldIdx));
	}


	private String getFieldText(IFieldDetail fldDef) {
		int start = fldDef.calculateActualPosition(this) - 1;
		String tData = data;

		int tempLen = fldDef.getLen();
		if (fldDef.getType() == Type.ftCharRestOfRecord || tempLen > tData.length() - start) {
			tempLen = tData.length() - start;
		}

	    if (tData.length() < start || tempLen < 1 || start < 0) {
	        return "";
	    }
		String s = tData.substring(start, start + tempLen);
		int len = s.length() - 1;
		if (s.length() > 0 && " ".equals(s.substring(len))) {
			while (len > 0 && " ".equals(s.substring(len - 1, len))) {
				len -= 1;
			}
			s = s.substring(0, len);
		}

		return s;
	}


	@Override
	public String getFullLine() {
		return data;
	}


	/**
	 * Get the Prefered Record Layou Index for this record
	 *
	 * @return Index of the Record Layout based on the Values
	 */
	@Override
	public int getPreferredLayoutIdx() {
		int ret = preferredLayout;

		if (ret == Constants.NULL_INTEGER) {
			ret = getPreferredLayoutIdxAlt();

			if (ret < 0) {
				for (int i=0; i< layout.getRecordCount(); i++) {
					if (this.data.length() == layout.getRecord(i).getLength()) {
						ret = i;
						preferredLayout = i;
						break;
					}
				}
			}
		}

		return ret;
	}


	@Override
	public void replace(byte[] rec, int start, int len) {

	}

	@Override
	public void setData(String newVal) {
		data = newVal;
	}



	@Override
	protected void setField(int typeId , IFieldDetail field, Object value) {

		if (field.isFixedFormat()) {
			String s = "";
			Type type = TypeManager.getSystemTypeManager().getType(typeId);
			if (value != null) {
				s = value.toString();
			}

			s = type.formatValueForRecord(field, s);

			int len = field.getLen();
			if (s.length() < len
			&&	type instanceof TypeChar && ! ((TypeChar) type).isLeftJustified()) {
				s = Conversion.padFront(s, len - s.length(), ' ');
			} 
			updateData(field.calculateActualPosition(this), len, s);
			
		} else {
	        ICsvLineParser parser = ParserManager.getInstance().get(field.getRecord().getRecordStyle());
	        Type typeVal = TypeManager.getSystemTypeManager().getType(typeId);
	        String s = typeVal.formatValueForRecord(field, value.toString());

            data =
            		parser.setField(field.calculateActualPosition(this) - 1,
            				typeVal.getFieldType(),
            				data,
            				new CsvDefinition(layout.getDelimiter(), field.getQuote()), s);
		}
	}

	@Override
	public String setFieldHex(int recordIdx, int fieldIdx, String val) {

		return null;
	}

	@Override
	public void setFieldText(int recordIdx, int fieldIdx, String value) {
		FieldDetail fldDef = layout.getRecord(recordIdx).getField(fieldIdx);

		updateData(fldDef.calculateActualPosition(this), fldDef.getLen(), value);
	}

	private void updateData(int pos, int length, String value) {
		int i;
		int start = pos -1;
		int len = Math.min(value.length(), length);
		StringBuilder dataBld = new StringBuilder(data);
		int en = start + Math.max(len, length) - dataBld.length();

		//System.out.print(" --> " + dataBld.length());
		for (i = 0; i <= en; i++) {
			dataBld.append(' ');
		}
		for (i = 0; i < len; i++) {
			dataBld.setCharAt(start + i, value.charAt(i));
		}

		for (i = len; i < length; i++) {
			dataBld.setCharAt(start + i, ' ');
		}
		data = dataBld.toString();
	}


    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
    	return lineProvider.getLine(layout, data);
    }

    

	@Override
	public boolean isDefined(IFieldDetail field) {
		if (this.data == null || data.length() <= field.getPos()) {
			return false;
		}

		if (TypeManager.isNumeric(field.getType())) {
			int e = Math.min(field.getPos() + field.getLen(), data.length());
			for (int i = field.getPos() - 1; i < e; i++) {
				switch (data.charAt(i)) {
				case ' ':
				case 0:
					break;
				default:
					return true;
				}
			}
			return false;
		}
		return true;
		
	}

}
