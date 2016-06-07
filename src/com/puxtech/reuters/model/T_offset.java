package com.puxtech.reuters.model;
import java.util.Date;

public class T_offset {
	// String sql =
	// "insert into T_OFFSET (COMMODITYID, OFFSET, RECTIME, NOWTIME, DIFFTIME, DOWNSET) values ('DAG', 12.230000, to_date('10-05-2015 16:39:17', 'dd-mm-yyyy hh24:mi:ss'), to_date('10-05-2015 16:39:17', 'dd-mm-yyyy hh24:mi:ss'), 12121, 12.000000) ";
	private String commodityId;
	private Double offset;
	private Date recTime;
	private Date nowTime;
	private Long diffTime;
	private Double downset;// À•ºı÷µ
	private Integer diffPricePeriod;//À•ºı÷‹∆⁄
	private String src;
	
	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	@Override
	public String toString() {
		return "T_offset [commodityId=" + commodityId + ", diffPricePeriod=" + diffPricePeriod + ", diffTime=" + diffTime + ", downset=" + downset + ", nowTime=" + nowTime
				+ ", offset=" + offset + ", recTime=" + recTime + "]";
	}

	public Integer getDiffPricePeriod() {
		return diffPricePeriod;
	}

	public void setDiffPricePeriod(Integer diffPricePeriod) {
		this.diffPricePeriod = diffPricePeriod;
	}

	public String getCommodityId() {
		return commodityId;
	}

	public void setCommodityId(String commodityId) {
		this.commodityId = commodityId;
	}

	public Double getOffset() {
		return offset;
	}

	public void setOffset(Double offset) {
		this.offset = offset;
	}

	public Date getRecTime() {
		return recTime;
	}

	public void setRecTime(Date recTime) {
		this.recTime = recTime;
	}

	public Date getNowTime() {
		return nowTime;
	}

	public void setNowTime(Date nowTime) {
		this.nowTime = nowTime;
	}

	public Long getDiffTime() {
		return diffTime;
	}

	public void setDiffTime(Long diffTime) {
		this.diffTime = diffTime;
	}

	public Double getDownset() {
		return downset;
	}

	public void setDownset(Double downset) {
		this.downset = downset;
	}

}
