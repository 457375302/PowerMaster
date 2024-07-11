import request from '@/config/axios'

// 通道列 VO
export interface IndexVO {
  id: number // 主键id
  roomId: number // 机房id
  name: string // 通道名称
  pduBar: number // 数据来源
  isDelete: number // 是否删除
  length: number // 长度
  type: string // 柜列类型
}

// 通道列 API
export const IndexApi = {
  // 查询通道列分页
  getIndexPage: async (data: any) => {
    return await request.post({ url: `/aisle/index/page`, data })
  },

  // 查询通道列详情
  getIndex: async (id: number) => {
    return await request.get({ url: `/aisle/index/get?id=` + id })
  },

  // 新增通道列
  createIndex: async (data: IndexVO) => {
    return await request.post({ url: `/aisle/index/create`, data })
  },

  // 修改通道列
  updateIndex: async (data: IndexVO) => {
    return await request.put({ url: `/aisle/index/update`, data })
  },

  // 删除通道列
  deleteIndex: async (id: number) => {
    return await request.delete({ url: `/aisle/index/delete?id=` + id })
  },

  // 导出通道列 Excel
  exportIndex: async (params) => {
    return await request.download({ url: `/aisle/index/export-excel`, params })
  },

  devKeyList: async () => {
    return await request.download({ url: `/aisle/index/devKeyList` })
  },

  getBusMenu: async () => {
    return await request.get({ url: `/room/bus/menu` })
  },
}
