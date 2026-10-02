import { InputNumber, type InputNumberProps } from 'antd'

/** O nhap tien VND: hien thi 1.500.000, tra ve so 1500000. */
export default function SoTienInput(props: InputNumberProps<number>) {
  return (
    <InputNumber<number>
      style={{ width: '100%' }}
      step={100000}
      formatter={(v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
      parser={(v) => Number((v ?? '').replace(/\./g, ''))}
      addonAfter="đ"
      {...props}
    />
  )
}
