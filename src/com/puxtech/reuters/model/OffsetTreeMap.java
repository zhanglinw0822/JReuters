package com.puxtech.reuters.model;

import java.util.TreeMap;
public class OffsetTreeMap<T, V> extends TreeMap<T, V> {

	private static final long serialVersionUID = 1L;

	@Override
	public boolean containsKey(Object key) {
		for(Object co : this.keySet()){
			ContractOffset c1 = (ContractOffset)co;
			ContractOffset c2 = (ContractOffset)key;
			if(c1.getOffset().equals(c2.getOffset())){
				return true;
			}
		}
		return false;
	}

}
