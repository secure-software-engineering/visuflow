package de.userstudy.analysis1;

import java.util.Arrays;

import soot.Local;
import soot.SootField;
import soot.Unit;

public class DataFlowFact {

	private final Unit source;
	private final Local local;
	private final SootField[] fields;
	private final static int MAX_ACCESSPATH_LENGTH = 4; 
	
	public DataFlowFact(Unit source, Local local) {
		this.source = source;
		this.local = local;
		this.fields = new SootField[0];
	}
	
	public DataFlowFact(Unit source, SootField field) {
		this(source, null, field);
	}
	
	public DataFlowFact(Unit source, Local local, SootField field) {
		this.source = source;
		this.local = local;
		this.fields = new SootField[1];
		this.fields[0] = field;
	}

	private DataFlowFact(Unit source,  SootField[] field, Local local) {
		this.source = source;
		this.local = local;
		this.fields = field;
	}
	
	public Unit getSource() {
		return this.source;
	}

	public Local getLocal() {
		return this.local;
	}

	public SootField[] getField() {
		return this.fields;
	}

	public SootField getFirstField() {
		if(this.fields == null || this.fields.length == 0)
			return null;
		return this.fields[0];
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((local == null) ? 0 : local.hashCode());
		result = prime * result + ((fields == null) ? 0 : Arrays.hashCode(fields));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof DataFlowFact))
			return false;
		DataFlowFact other = (DataFlowFact) obj;
		if (local == null) {
			if (other.local != null)
				return false;
		} else if (!local.equals(other.local))
			return false;
		if (fields == null) {
			if (other.fields != null)
				return false;
		} else if (!Arrays.equals(fields,other.fields))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String res = "";
		if (local != null)
			res += "LOCAL " + local;
		if (fields != null)
			res += " FIELD " + Arrays.toString(fields);
		return res;
	}
	
	public DataFlowFact deriveWithNewSource(Unit newSource) {
		return new DataFlowFact(newSource, fields,local);
	}

	private DataFlowFact deriveAndAppendField(SootField field){
		SootField[] newFields;
		if(this.fields.length == MAX_ACCESSPATH_LENGTH){
			newFields = new SootField[this.fields.length];
			System.arraycopy(fields, 0, newFields, 1, this.fields.length-1);
		} else {
			newFields = new SootField[this.fields.length + 1];
			System.arraycopy(fields, 0, newFields, 1, this.fields.length);
		}
		newFields[0] = field;
		return new DataFlowFact(this.source, newFields,this.local);
	}
	
	public DataFlowFact deriveWithNewLocal(Local newLocal){
		SootField[] newFields = new SootField[this.fields.length];
		System.arraycopy(fields, 0, newFields, 0, this.fields.length);
		return new DataFlowFact(this.source,newFields, newLocal);
	}
	public DataFlowFact deriveWithNewLocalAndAppendField(Local newLocal, SootField field){
		DataFlowFact nL = deriveWithNewLocal(newLocal);
		return nL.deriveAndAppendField(field);
	}
	

	public DataFlowFact deriveWithNewLocalAndPopFirstField(Local newLocal){
		DataFlowFact accesspath = deriveWithNewLocal(newLocal);
		return accesspath.popFirstField();
	}
	
	private DataFlowFact popFirstField(){
		if (this.fields == null || this.fields.length == 0)
			return this;
		
		if (this.fields.length > 1) {
			SootField[] newFields = new SootField[this.fields.length - 1];
			System.arraycopy(fields, 1, newFields, 0, this.fields.length-1);
			return new DataFlowFact(this.source, newFields, this.local);
		}
		
		return new DataFlowFact(this.source, this.local);
	}

}
