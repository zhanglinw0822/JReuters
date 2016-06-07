create user HQBJ identified by password default tablespace users temporary tablespace temp;
grant connect, resource, create table, create view, debug connect session,create synonym to HQBJ;


-------------------------------------------------
-- Export file for user REUTERS@172.31.100.212 --
-- Created by Smith on 2015/6/6, 22:23:13 -------
-------------------------------------------------

set define off
spool 1.log

prompt

prompt Creating table T_CONTRACTOFFSET
prompt ===============================
prompt

create table HQBJ.T_CONTRACTOFFSET
(
  offset           NUMBER(10,5),
  occurtime        DATE not null,
  src              VARCHAR2(100) not null,
  oldcontract      VARCHAR2(100) not null,
  newcontract      VARCHAR2(100) not null,
  worldcommodityid VARCHAR2(100) not null,
  switchdate       VARCHAR2(8) not null
)
tablespace USERS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
comment on column HQBJ.T_CONTRACTOFFSET.offset
  is 'ƫ����';
comment on column HQBJ.T_CONTRACTOFFSET.occurtime
  is '����ʱ��';
comment on column HQBJ.T_CONTRACTOFFSET.src
  is '��Դ:(ip:�˿�)';
comment on column HQBJ.T_CONTRACTOFFSET.oldcontract
  is '�ɺ�Լ����';
comment on column HQBJ.T_CONTRACTOFFSET.newcontract
  is '�º�Լ����';
comment on column HQBJ.T_CONTRACTOFFSET.worldcommodityid
  is '������Ʒ����';
comment on column HQBJ.T_CONTRACTOFFSET.switchdate
  is '�л�����';
alter table HQBJ.T_CONTRACTOFFSET
  add constraint PRIMARY_T_CONTRACTOFFSET primary key (SRC, WORLDCOMMODITYID, SWITCHDATE)
  using index 
  tablespace USERS
  pctfree 10
  initrans 2
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );

prompt

prompt Creating table T_DBLOG
prompt ======================
prompt

create table HQBJ.T_DBLOG
(
  err_date  DATE not null,
  name_proc VARCHAR2(30),
  err_code  NUMBER(10),
  err_msg   VARCHAR2(200)
)
tablespace USERS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
comment on table HQBJ.T_DBLOG
  is 'ִ�д洢��־��
ִ�д洢��־��';
comment on column HQBJ.T_DBLOG.err_date
  is '��¼ʱ��';
comment on column HQBJ.T_DBLOG.name_proc
  is '�洢����';
comment on column HQBJ.T_DBLOG.err_code
  is '�������';
comment on column HQBJ.T_DBLOG.err_msg
  is '������Ϣ';

prompt

prompt Creating table T_OFFSET
prompt =======================
prompt

create table HQBJ.T_OFFSET
(
  commodityid     VARCHAR2(20),
  offset          NUMBER(10,6),
  rectime         DATE,
  nowtime         DATE,
  difftime        NUMBER(12),
  downset         NUMBER(12,6),
  diffpriceperiod NUMBER(20),
  src             VARCHAR2(20)
)
tablespace USERS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
comment on column HQBJ.T_OFFSET.commodityid
  is '��Ʒid';
comment on column HQBJ.T_OFFSET.offset
  is '�۲�ֵ';
comment on column HQBJ.T_OFFSET.rectime
  is '�۲�Э�����ʱ��';
comment on column HQBJ.T_OFFSET.nowtime
  is '��ǰʱ��';
comment on column HQBJ.T_OFFSET.difftime
  is 'ʱ���';
comment on column HQBJ.T_OFFSET.downset
  is '˥��ֵ';
comment on column HQBJ.T_OFFSET.diffpriceperiod
  is '˥������';
comment on column HQBJ.T_OFFSET.src
  is '����ԴID';

prompt

prompt Creating table T_SPREAD
prompt =======================
prompt

create table HQBJ.T_SPREAD
(
  exchangecode       VARCHAR2(20) not null,
  quotesource        VARCHAR2(20) not null,
  spreadconsumer     VARCHAR2(200) not null,
  spreadindex        NUMBER not null,
  contractcode       VARCHAR2(100),
  attenbegintime     DATE,
  changecontracttime DATE,
  attenendtime       DATE
)
tablespace USERS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
comment on column HQBJ.T_SPREAD.exchangecode
  is '�������';
comment on column HQBJ.T_SPREAD.quotesource
  is '����Դ�������·͸�ȣ�';
comment on column HQBJ.T_SPREAD.spreadconsumer
  is '����������·��';
comment on column HQBJ.T_SPREAD.spreadindex
  is '����';
comment on column HQBJ.T_SPREAD.contractcode
  is '��Լ����';
comment on column HQBJ.T_SPREAD.attenbegintime
  is '�۲�˥����ʼʱ��';
comment on column HQBJ.T_SPREAD.changecontracttime
  is '�к�Լʱ��';
comment on column HQBJ.T_SPREAD.attenendtime
  is '˥������ʱ��';
alter table HQBJ.T_SPREAD
  add constraint PRIMARY_T_SPREAD primary key (EXCHANGECODE, QUOTESOURCE, SPREADCONSUMER, SPREADINDEX)
  using index 
  tablespace USERS
  pctfree 10
  initrans 2
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );


spool off

