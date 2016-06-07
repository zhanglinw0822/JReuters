package com.puxtech.dc.common;

public interface CallbackBehaviour {
	void OnDCConnect(String pszSerAddr, boolean bConnectSuc);
	/**********************************************************************
	* 函数名称:	OnDCClose
	* 功能描述:	连接关闭
	* 输入参数:	void *pUser: 回调数据
	*			char *pszSerAddr: 服务器地址
	* 输出参数:	NA
	* 返 回 值:	返回NULL表示失败
	* 其它说明:	NA
	* 历史记录:	原始版本
	***********************************************************************/
	void OnDCClose(String pszSerAddr);

	/**********************************************************************
	* 函数名称:	OnDCAuth
	* 功能描述:	登录返回
	* 输入参数:	void *pUser: 用户数据
	*			char *pszSerAddr: 服务器地址
	*			bool bAuthSuc: 是否登录成功
	*			char *pszErrMsg: 如果登录失败，会是失败消息, 可能为NULL
	* 输出参数:	NA
	* 返 回 值:	返回NULL表示失败
	* 其它说明:	NA
	* 历史记录:	原始版本
	***********************************************************************/
	void OnDCAuth(String pszSerAddr, boolean bAuthSuc, String pszErrMsg);

	/**********************************************************************
	* 函数名称:	OnDCLog
	* 功能描述:	显示一些运行中的日志
	* 输入参数:	void *pUser: 用户数据
	*			char *pszSerAddr: 服务器地址
	*			char *pszMsg: 错误消息
	* 输出参数:	NA
	* 返 回 值:	返回NULL表示失败
	* 其它说明:	NA
	* 历史记录:	原始版本
	***********************************************************************/
	void OnDCLog(String pszSerAddr, String pszMsg);

	/**********************************************************************
	* 函数名称:	OnDCPrice
	* 功能描述:	行情到来
	* 输入参数:	void *pUser: 用户数据
	*			char *pszSerAddr: 服务器地址
	*			T_DCPricePacket *pstPrice: 行情
	* 输出参数:	NA
	* 返 回 值:	返回NULL表示失败
	* 其它说明:	NA
	* 历史记录:	原始版本
	***********************************************************************/
	void OnDCPrice(String pszSerAddr, String market, String code, int updateFlag, double price, long time);

	/**********************************************************************
	* 函数名称:	OnDCFinance
	* 功能描述:	财务信息到来
	* 输入参数:	void *pUser: 用户数据
	*			char *pszSerAddr: 服务器地址
	*			T_DCSymFinanceInfo *pstFinance: 财务信息
	* 输出参数:	NA
	* 返 回 值:	返回NULL表示失败
	* 其它说明:	NA
	* 历史记录:	原始版本
	***********************************************************************/
	void OnDCFinance(String pszSerAddr, String pstFinance);

	/**********************************************************************
	* 函数名称:	OnDCInfo
	* 功能描述:	资讯到来
	* 输入参数:	void *pUser: 用户数据
	*			char *pszSerAddr: 服务器地址
	*			int iInfoType: 资讯类型
	*			T_DCInfoIndex *pstIndex: 索引
	*			int iIndexCouont: 索引个数
	*			char *pszContentName: 内容
	*			int iContentLen: 内容长度
	* 输出参数:	NA
	* 返 回 值:	返回NULL表示失败
	* 其它说明:	NA
	* 历史记录:	原始版本
	***********************************************************************/
//	void OnDCInfo(String pszSerAddr, int iInfoType, IN const T_DCInfoIndex *pstIndex, IN int iIndexCount, IN const char *pszContent, IN int iContentLen);
}
