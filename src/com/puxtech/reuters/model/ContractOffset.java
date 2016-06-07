package com.puxtech.reuters.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 价差对象
 * 
 * @author Smith
 * 
 */
public class ContractOffset implements Comparable<ContractOffset>, Serializable {
	private static final long serialVersionUID = 8792739472934723L;
	private Double offset;
	private Date occurtime;
	private String src;
	private String oldContract;
	private String newContract;
	private String worldCommodityId;
	private String switchDate;

	public String getSwitchDate() {
		return switchDate;
	}

	public void setSwitchDate(String switchDate) {
		this.switchDate = switchDate;
	}

	public String getWorldCommodityId() {
		return worldCommodityId;
	}

	public void setWorldCommodityId(String worldCommodityId) {
		this.worldCommodityId = worldCommodityId;
	}

	public Double getOffset() {
		return offset;
	}

	public void setOffset(Double offset) {
		this.offset = offset;
	}

	public Date getOccurtime() {
		return occurtime;
	}

	public void setOccurtime(Date occurtime) {
		this.occurtime = occurtime;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getOldContract() {
		return oldContract;
	}

	public void setOldContract(String oldContract) {
		this.oldContract = oldContract;
	}

	public String getNewContract() {
		return newContract;
	}

	public void setNewContract(String newContract) {
		this.newContract = newContract;
	}

	@Override
	// 主键：NEWCONTRACT, OLDCONTRACT, SWITCHDATE, SRC, WORLDCOMMODITYID
	public boolean equals(Object obj) {
		if (obj instanceof ContractOffset) {
			ContractOffset in = (ContractOffset) obj;
			if (in.getSwitchDate().equals(switchDate)
					&& in.getSrc().equals(src)
					&& in.getWorldCommodityId().equals(worldCommodityId)
					&& in.getOldContract().equals(oldContract)
					&& in.getNewContract().equals(newContract)) {
				return true;
			}
		} else {
			return false;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public ContractOffset(Double offset, Date occurtime, String src,
			String oldContract, String newContract, String worldCommodityId,
			String switchDate) {
		super();
		this.offset = offset;
		this.occurtime = occurtime;
		this.src = src;
		this.oldContract = oldContract;
		this.newContract = newContract;
		this.worldCommodityId = worldCommodityId;
		this.switchDate = switchDate;
	}

	public ContractOffset() {

	}

	@Override
	public String toString() {
		return "ContractOffset [newContract=" + newContract + ", occurtime="
				+ occurtime + ", offset=" + offset + ", oldContract="
				+ oldContract + ", src=" + src + ", switchDate=" + switchDate
				+ ", worldCommodityId=" + worldCommodityId + "]";
	}

	@Override
	public int compareTo(ContractOffset o) {
		Double tempObj = ((ContractOffset) o).getOffset();
		return ((Double) offset).compareTo(tempObj);
	}

}
