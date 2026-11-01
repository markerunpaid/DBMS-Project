import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useAuth } from './AuthContext.jsx'

const CartContext = createContext(null)
const EMPTY = { owner: null, store: null, addressId: null, items: {} }   // items: productId -> { product, qty }

// one saved cart per user, so tabs logged in as different users never overwrite each other
const keyFor = (owner) => `qc_cart:${owner}`

function load(owner) {
  if (!owner) return EMPTY
  try {
    const saved = JSON.parse(localStorage.getItem(keyFor(owner)))
    if (saved) return { ...saved, owner }
  } catch {
    // corrupted storage -- start empty
  }
  return { ...EMPTY, owner }
}

export function CartProvider({ children }) {
  const { user } = useAuth()
  const owner = user ? `${user.role}:${user.id}` : null
  const [cart, setCart] = useState(() => load(owner))

  // a different user logged in -> their own (or an empty) cart
  useEffect(() => {
    setCart((c) => (c.owner === owner ? c : load(owner)))
  }, [owner])

  useEffect(() => {
    if (cart.owner) localStorage.setItem(keyFor(cart.owner), JSON.stringify(cart))
  }, [cart])

  const value = useMemo(() => {
    const lines = Object.values(cart.items)
    return {
      cart,
      lines,
      count: lines.reduce((n, l) => n + l.qty, 0),
      subtotal: lines.reduce((s, l) => s + l.qty * Number(l.product.price), 0),

      setAddressId(addressId) {
        setCart((c) => ({ ...c, addressId }))
      },

      /** Legacy single-store starter; optional in multi-store mode. */
      startShopping(store, addressId, { force = false } = {}) {
        const sameStore = cart.store?.id === store.id
        if (!sameStore && lines.length > 0 && !force) return false
        setCart((c) => ({
          ...c,
          store,
          addressId,
          items: c.store?.id === store.id ? c.items : {},
        }))
        return true
      },

      setQty(product, qty, addressId) {
        setCart((c) => {
          const items = { ...c.items }
          const maxAvail = product.totalAvailable ?? product.available ?? 999
          if (qty <= 0) {
            delete items[product.productId]
          } else {
            items[product.productId] = { product, qty: Math.min(qty, maxAvail) }
          }
          return {
            ...c,
            addressId: addressId || c.addressId,
            items,
          }
        })
      },

      qtyOf(productId) {
        return cart.items[productId]?.qty || 0
      },

      clear() {
        setCart((c) => ({ ...EMPTY, owner: c.owner, addressId: c.addressId }))
      },
    }
  }, [cart])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  return useContext(CartContext)
}
