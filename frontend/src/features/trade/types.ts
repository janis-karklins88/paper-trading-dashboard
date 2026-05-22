export type SelectedAsset = {
  symbol: string
  name: string
  quoteSymbol: string
}

export type ChartLevel = {
  id: string
  price: number
  label: string
  color: string
  lineStyle: 'solid' | 'dashed'
}
