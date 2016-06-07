package com.puxtech.dc.common;

public interface CallbackBehaviour {
	void OnDCConnect(String pszSerAddr, boolean bConnectSuc);
	/**********************************************************************
	* ��������:	OnDCClose
	* ��������:	���ӹر�
	* �������:	void *pUser: �ص�����
	*			char *pszSerAddr: ��������ַ
	* �������:	NA
	* �� �� ֵ:	����NULL��ʾʧ��
	* ����˵��:	NA
	* ��ʷ��¼:	ԭʼ�汾
	***********************************************************************/
	void OnDCClose(String pszSerAddr);

	/**********************************************************************
	* ��������:	OnDCAuth
	* ��������:	��¼����
	* �������:	void *pUser: �û�����
	*			char *pszSerAddr: ��������ַ
	*			bool bAuthSuc: �Ƿ��¼�ɹ�
	*			char *pszErrMsg: �����¼ʧ�ܣ�����ʧ����Ϣ, ����ΪNULL
	* �������:	NA
	* �� �� ֵ:	����NULL��ʾʧ��
	* ����˵��:	NA
	* ��ʷ��¼:	ԭʼ�汾
	***********************************************************************/
	void OnDCAuth(String pszSerAddr, boolean bAuthSuc, String pszErrMsg);

	/**********************************************************************
	* ��������:	OnDCLog
	* ��������:	��ʾһЩ�����е���־
	* �������:	void *pUser: �û�����
	*			char *pszSerAddr: ��������ַ
	*			char *pszMsg: ������Ϣ
	* �������:	NA
	* �� �� ֵ:	����NULL��ʾʧ��
	* ����˵��:	NA
	* ��ʷ��¼:	ԭʼ�汾
	***********************************************************************/
	void OnDCLog(String pszSerAddr, String pszMsg);

	/**********************************************************************
	* ��������:	OnDCPrice
	* ��������:	���鵽��
	* �������:	void *pUser: �û�����
	*			char *pszSerAddr: ��������ַ
	*			T_DCPricePacket *pstPrice: ����
	* �������:	NA
	* �� �� ֵ:	����NULL��ʾʧ��
	* ����˵��:	NA
	* ��ʷ��¼:	ԭʼ�汾
	***********************************************************************/
	void OnDCPrice(String pszSerAddr, String market, String code, int updateFlag, double price, long time);

	/**********************************************************************
	* ��������:	OnDCFinance
	* ��������:	������Ϣ����
	* �������:	void *pUser: �û�����
	*			char *pszSerAddr: ��������ַ
	*			T_DCSymFinanceInfo *pstFinance: ������Ϣ
	* �������:	NA
	* �� �� ֵ:	����NULL��ʾʧ��
	* ����˵��:	NA
	* ��ʷ��¼:	ԭʼ�汾
	***********************************************************************/
	void OnDCFinance(String pszSerAddr, String pstFinance);

	/**********************************************************************
	* ��������:	OnDCInfo
	* ��������:	��Ѷ����
	* �������:	void *pUser: �û�����
	*			char *pszSerAddr: ��������ַ
	*			int iInfoType: ��Ѷ����
	*			T_DCInfoIndex *pstIndex: ����
	*			int iIndexCouont: ��������
	*			char *pszContentName: ����
	*			int iContentLen: ���ݳ���
	* �������:	NA
	* �� �� ֵ:	����NULL��ʾʧ��
	* ����˵��:	NA
	* ��ʷ��¼:	ԭʼ�汾
	***********************************************************************/
//	void OnDCInfo(String pszSerAddr, int iInfoType, IN const T_DCInfoIndex *pstIndex, IN int iIndexCount, IN const char *pszContent, IN int iContentLen);
}
