import { useState, useRef, useEffect } from 'react'

// ─── Theme definitions ───────────────────────────────────────────────────────

type ThemeName = 'modern' | 'retro'

interface ThemePalette {
  name: ThemeName
  // semantic roles used across all components
  primary: string        // brand / header bg
  primaryText: string    // text on primary bg
  accent: string         // secondary highlight
  bg: string             // page / widget background
  surface: string        // content area (messages)
  surfaceAlt: string     // hover / alternate surface
  text: string           // main body text
  textMuted: string      // secondary / muted text
  price: string          // price emphasis
  success: string        // confirmation green
  successBg: string      // confirmation bg
  border: string         // general border
  font: string
}

const MODERN: ThemePalette = {
  name: 'modern',
  primary:    '#6db33f',
  primaryText:'#ffffff',
  accent:     '#34c75a',
  bg:         '#f3f4f6',
  surface:    '#ffffff',
  surfaceAlt: '#f0f7ea',
  text:       '#1f2937',
  textMuted:  '#6b7280',
  price:      '#6db33f',
  success:    '#3d7a1f',
  successBg:  '#f0f7ea',
  border:     '#e5e7eb',
  font:       'system-ui, -apple-system, sans-serif',
}

const RETRO: ThemePalette = {
  name:       'retro',
  primary:    '#000080',
  primaryText:'#ffff00',
  accent:     '#008080',
  bg:         '#c0c0c0',
  surface:    '#ffffff',
  surfaceAlt: '#dfdfdf',
  text:       '#000000',
  textMuted:  '#808080',
  price:      '#800000',
  success:    '#000080',
  successBg:  '#ffffff',
  border:     '#808080',
  font:       '"Comic Sans MS", "Trebuchet MS", Verdana, Arial, sans-serif',
}

// ─── Style helpers ────────────────────────────────────────────────────────────

function getRaisedBox(t: ThemePalette): React.CSSProperties {
  if (t.name === 'retro') {
    return {
      border: '2px solid',
      borderColor: `#ffffff #808080 #808080 #ffffff`,
      background: t.bg,
    }
  }
  return {
    border: `1px solid ${t.border}`,
    borderRadius: '10px',
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
    background: t.surface,
  }
}

function getInsetBox(t: ThemePalette): React.CSSProperties {
  if (t.name === 'retro') {
    return {
      border: '2px solid',
      borderColor: `#808080 #ffffff #ffffff #808080`,
      background: '#ffffff',
    }
  }
  return {
    border: `1px solid #d1d5db`,
    borderRadius: '8px',
    background: '#ffffff',
  }
}

// ─── Data interfaces ──────────────────────────────────────────────────────────

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

interface MerchCard {
  projectName: string
  type: string
  price: number
  stock: number
  logoUrl: string
}

interface OrderPlaced {
  orderId: string
  items: { name: string; quantity: number; unitPrice: number }[]
  total: number
}

function extractMerchCards(content: string): { cards: MerchCard[]; cleanContent: string } {
  const match = content.match(/<merch-items>([\s\S]*?)<\/merch-items>/)
  if (!match) return { cards: [], cleanContent: content }
  try {
    const cards: MerchCard[] = JSON.parse(match[1])
    return { cards, cleanContent: content.replace(match[0], '').trim() }
  } catch {
    return { cards: [], cleanContent: content }
  }
}

function extractOrderPlaced(content: string): { order: OrderPlaced | null; cleanContent: string } {
  const match = content.match(/<order-placed>([\s\S]*?)<\/order-placed>/)
  if (!match) return { order: null, cleanContent: content }
  try {
    const order: OrderPlaced = JSON.parse(match[1])
    return { order, cleanContent: content.replace(match[0], '').trim() }
  } catch {
    return { order: null, cleanContent: content }
  }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function MerchCardItem({
  card, theme, onAddToOrder,
}: {
  card: MerchCard
  theme: ThemePalette
  onAddToOrder: (card: MerchCard) => void
}) {
  const [hovered, setHovered] = useState(false)
  const [active, setActive] = useState(false)
  const isRetro = theme.name === 'retro'
  const raised = getRaisedBox(theme)

  const activeBevel: React.CSSProperties = isRetro
    ? { borderColor: '#808080 #ffffff #ffffff #808080' }
    : { boxShadow: 'inset 0 1px 3px rgba(0,0,0,0.15)' }

  return (
    <div
      onClick={() => onAddToOrder(card)}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => { setHovered(false); setActive(false) }}
      onMouseDown={() => setActive(true)}
      onMouseUp={() => setActive(false)}
      title={`Click to add ${card.projectName} ${card.type} to your order`}
      style={{
        ...raised,
        ...(active ? activeBevel : {}),
        ...(hovered && !isRetro ? { background: theme.surfaceAlt, borderColor: theme.primary } : {}),
        ...(hovered && isRetro ? { background: theme.surfaceAlt } : {}),
        padding: '8px',
        width: '130px',
        textAlign: 'center',
        fontSize: '11px',
        color: theme.text,
        cursor: 'pointer',
        userSelect: 'none',
        transition: isRetro ? undefined : 'all 0.15s ease',
      }}
    >
      <img
        src={card.logoUrl}
        alt={`${card.projectName} ${card.type}`}
        style={{
          width: '56px',
          height: '56px',
          objectFit: 'contain',
          marginBottom: '4px',
          border: isRetro ? `1px solid ${theme.textMuted}` : `1px solid ${theme.border}`,
          borderRadius: isRetro ? 0 : '6px',
          background: theme.surface,
          padding: '2px',
        }}
        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
      />
      <div style={{ fontWeight: 'bold', marginBottom: '2px', fontSize: '10px', color: theme.success }}>
        {card.projectName} {card.type}
      </div>
      <div style={{ color: theme.price, fontWeight: 'bold', fontSize: '12px' }}>
        ${card.price.toFixed(2)}
      </div>
      <div style={{ color: theme.textMuted, fontSize: '10px', marginBottom: '6px' }}>
        {card.stock} in stock
      </div>
      <div style={{
        ...(isRetro ? { ...raised, ...(active ? activeBevel : {}), display: 'inline-block' } : {
          display: 'inline-block',
          background: hovered ? theme.primary : theme.surfaceAlt,
          color: hovered ? theme.primaryText : theme.primary,
          borderRadius: '5px',
          transition: 'all 0.15s ease',
        }),
        padding: isRetro ? '2px 8px' : '3px 8px',
        fontSize: '10px',
        fontWeight: 'bold',
        color: isRetro ? theme.text : undefined,
      }}>
        {isRetro ? '>> Add' : '+ Add to order'}
      </div>
    </div>
  )
}

function OrderPlacedCard({ order, theme }: { order: OrderPlaced; theme: ThemePalette }) {
  const isRetro = theme.name === 'retro'
  return (
    <div style={{
      ...getInsetBox(theme),
      padding: '10px 14px',
      marginBottom: '8px',
      fontSize: '12px',
      color: theme.text,
    }}>
      <div style={{
        background: theme.success,
        color: isRetro ? theme.primaryText : theme.primaryText,
        padding: '4px 8px',
        fontWeight: 'bold',
        fontSize: '13px',
        marginBottom: '8px',
        textAlign: 'center',
        letterSpacing: isRetro ? '1px' : '0.5px',
        borderRadius: isRetro ? 0 : '6px',
      }}>
        {isRetro ? `*** ORDER CONFIRMED! *** #${order.orderId}` : `🎉 Order Placed! #${order.orderId}`}
      </div>
      <div>
        {order.items.map((item, i) => (
          <div key={i} style={{
            display: 'flex',
            justifyContent: 'space-between',
            marginBottom: '4px',
            borderBottom: `1px ${isRetro ? 'dotted' : 'solid'} ${theme.border}`,
            paddingBottom: '3px',
          }}>
            <span>{item.quantity}× {item.name}</span>
            <span style={{ color: theme.price, fontWeight: 'bold' }}>
              ${(item.quantity * item.unitPrice).toFixed(2)}
            </span>
          </div>
        ))}
      </div>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        fontWeight: 'bold',
        fontSize: '13px',
        marginTop: '6px',
        color: theme.success,
      }}>
        <span>TOTAL:</span>
        <span>${order.total.toFixed(2)}</span>
      </div>
    </div>
  )
}

function MessageBubble({
  msg, theme, onAddToOrder,
}: {
  msg: ChatMessage
  theme: ThemePalette
  onAddToOrder: (card: MerchCard) => void
}) {
  const isUser = msg.role === 'user'
  const isRetro = theme.name === 'retro'
  const { cards, cleanContent: afterMerch } = !isUser
    ? extractMerchCards(msg.content)
    : { cards: [], cleanContent: msg.content }
  const { order, cleanContent } = !isUser
    ? extractOrderPlaced(afterMerch)
    : { order: null, cleanContent: afterMerch }

  const userStyle: React.CSSProperties = isRetro
    ? { background: theme.primary, color: theme.primaryText, border: `2px solid ${theme.accent}`, padding: '6px 12px' }
    : { background: theme.primary, color: theme.primaryText, borderRadius: '12px', padding: '10px 14px' }

  const assistantStyle: React.CSSProperties = isRetro
    ? { ...getRaisedBox(theme), padding: '8px 12px' }
    : { background: theme.bg, color: theme.text, border: `1px solid ${theme.border}`, borderRadius: '12px', padding: '10px 14px' }

  return (
    <div style={{ display: 'flex', justifyContent: isUser ? 'flex-end' : 'flex-start', marginBottom: '10px' }}>
      <div style={{
        maxWidth: '80%',
        ...(isUser ? userStyle : assistantStyle),
        fontSize: '13px',
        whiteSpace: 'pre-wrap',
        lineHeight: '1.5',
      }}>
        {!isUser && isRetro && (
          <div style={{ fontSize: '9px', color: theme.textMuted, marginBottom: '4px', letterSpacing: '1px', textTransform: 'uppercase' }}>
            [ SpringBot v2.0 says: ]
          </div>
        )}
        {cards.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: cleanContent ? '10px' : '0' }}>
            {cards.map((card, i) => (
              <MerchCardItem key={i} card={card} theme={theme} onAddToOrder={onAddToOrder} />
            ))}
          </div>
        )}
        {order && <OrderPlacedCard order={order} theme={theme} />}
        {cleanContent}
      </div>
    </div>
  )
}

// ─── Theme toggle button ──────────────────────────────────────────────────────

function ThemeToggle({ theme, onToggle }: { theme: ThemePalette; onToggle: () => void }) {
  const [hovered, setHovered] = useState(false)
  const isRetro = theme.name === 'retro'

  if (isRetro) {
    return (
      <button
        onClick={onToggle}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        style={{
          ...getRaisedBox(theme),
          marginLeft: 'auto',
          padding: '1px 10px',
          fontSize: '11px',
          fontWeight: 'bold',
          cursor: 'pointer',
          color: '#000000',
          background: hovered ? '#dfdfdf' : '#c0c0c0',
          fontFamily: theme.font,
          border: '2px solid',
          borderColor: hovered
            ? '#808080 #ffffff #ffffff #808080'
            : '#ffffff #808080 #808080 #ffffff',
        }}
        title="Switch to modern theme"
      >
        ✨ Modern
      </button>
    )
  }

  return (
    <button
      onClick={onToggle}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        marginLeft: 'auto',
        padding: '4px 12px',
        fontSize: '12px',
        fontWeight: 'bold',
        cursor: 'pointer',
        color: hovered ? theme.primaryText : theme.primary,
        background: hovered ? 'rgba(255,255,255,0.25)' : 'rgba(255,255,255,0.15)',
        border: `1px solid rgba(255,255,255,0.5)`,
        borderRadius: '999px',
        transition: 'all 0.15s ease',
        fontFamily: theme.font,
      }}
      title="Switch to retro theme"
    >
      🕹️ Retro
    </button>
  )
}

// ─── Main App ─────────────────────────────────────────────────────────────────

export default function App() {
  const [themeName, setThemeName] = useState<ThemeName>('modern')
  const theme = themeName === 'retro' ? RETRO : MODERN
  const isRetro = theme.name === 'retro'

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content: 'Welcome to the Spring Merch Store! Here you can order merch from different Spring projects. Ask me about t-shirts, socks, or stickers for any Spring project.',
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [conversationId] = useState(() => crypto.randomUUID())
  const bottomRef = useRef<HTMLDivElement>(null)
  const [tick, setTick] = useState(true)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (!isRetro) return
    const id = setInterval(() => setTick(t => !t), 600)
    return () => clearInterval(id)
  }, [isRetro])

  function toggleTheme() {
    setThemeName(t => t === 'modern' ? 'retro' : 'modern')
  }

  async function sendMessage(overrideText?: string) {
    const text = (overrideText ?? input).trim()
    if (!text || loading) return

    setMessages(prev => [...prev, { role: 'user', content: text }])
    if (!overrideText) setInput('')
    setLoading(true)
    setMessages(prev => [...prev, { role: 'assistant', content: '' }])

    try {
      const res = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId, message: text }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)

      const reader = res.body!.getReader()
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        for (const line of chunk.split('\n')) {
          if (line.startsWith('data:')) {
            const token = line.slice(5)
            setMessages(prev => {
              const updated = [...prev]
              updated[updated.length - 1] = {
                ...updated[updated.length - 1],
                content: updated[updated.length - 1].content + token,
              }
              return updated
            })
          }
        }
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error'
      setMessages(prev => {
        const updated = [...prev]
        updated[updated.length - 1] = { role: 'assistant', content: `Sorry, something went wrong: ${msg}` }
        return updated
      })
    } finally {
      setLoading(false)
    }
  }

  function handleAddToOrder(card: MerchCard) {
    sendMessage(`Add 1 ${card.projectName} ${card.type} to my order`)
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  // ── Retro chrome ─────────────────────────────────────────────────────────

  const retroChrome = (
    <>
      {/* Win9x title bar */}
      <div style={{
        background: 'linear-gradient(to right, #000080, #1084d0)',
        color: '#ffffff',
        padding: '3px 6px',
        fontSize: '12px',
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        gap: '6px',
        flexShrink: 0,
        userSelect: 'none',
      }}>
        <span style={{ fontSize: '14px' }}>🌱</span>
        Spring Merch Store — Microsoft Internet Explorer
        <div style={{ marginLeft: 'auto', display: 'flex', gap: '2px' }}>
          {['_', '□', '✕'].map(btn => (
            <div key={btn} style={{
              border: '2px solid', borderColor: '#ffffff #808080 #808080 #ffffff',
              background: '#c0c0c0', color: '#000000',
              width: '18px', height: '16px', fontSize: '9px',
              display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
            }}>{btn}</div>
          ))}
        </div>
      </div>

      {/* Menu bar */}
      <div style={{
        background: '#c0c0c0',
        borderBottom: '1px solid #808080',
        padding: '2px 8px',
        fontSize: '12px',
        color: '#000000',
        display: 'flex',
        gap: '16px',
        flexShrink: 0,
        alignItems: 'center',
      }}>
        {['File', 'Edit', 'View', 'Favorites', 'Tools', 'Help'].map(item => (
          <span key={item} style={{ cursor: 'default', padding: '1px 4px' }}>{item}</span>
        ))}
        <ThemeToggle theme={theme} onToggle={toggleTheme} />
      </div>

      {/* Address bar */}
      <div style={{
        background: '#c0c0c0',
        borderBottom: '2px solid #808080',
        padding: '3px 8px',
        fontSize: '11px',
        color: '#000000',
        display: 'flex',
        alignItems: 'center',
        gap: '6px',
        flexShrink: 0,
      }}>
        <span style={{ color: '#808080' }}>Address</span>
        <div style={{
          border: '2px solid', borderColor: '#808080 #ffffff #ffffff #808080',
          background: '#ffffff', flex: 1, padding: '1px 6px', fontSize: '11px', color: '#000080',
        }}>
          http://www.spring-merch-store.com/shop/index.html
        </div>
        <div style={{
          border: '2px solid', borderColor: '#ffffff #808080 #808080 #ffffff',
          background: '#c0c0c0', padding: '1px 10px', fontSize: '11px', cursor: 'pointer', color: '#000000',
        }}>Go</div>
      </div>

      {/* Banner */}
      <div style={{ background: '#000080', padding: '8px 0', flexShrink: 0, overflow: 'hidden' }}>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px',
          color: '#ffff00', fontSize: '20px', fontWeight: 'bold',
          textShadow: '2px 2px #008080', letterSpacing: '2px',
        }}>
          <span>✦</span><span>★ SPRING MERCH STORE ★</span><span>✦</span>
        </div>
        <div style={{ color: '#00ff00', fontSize: '11px', marginTop: '4px', textAlign: 'center' }}>
          🔥 HOT DEALS ON SPRING FRAMEWORK MERCH 🔥 &nbsp;&nbsp; ⭐ FREE SHIPPING ON ORDERS OVER $50 ⭐
        </div>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', marginTop: '6px', fontSize: '11px' }}>
          {['HOME', 'PRODUCTS', 'MY CART', 'CONTACT US', 'GUESTBOOK'].map(link => (
            <span key={link} style={{ color: '#ffff00', cursor: 'pointer', textDecoration: 'underline', fontWeight: 'bold' }}>{link}</span>
          ))}
        </div>
      </div>

      <hr style={{ border: 'none', borderTop: '2px inset #808080', flexShrink: 0 }} />
    </>
  )

  // ── Modern chrome ─────────────────────────────────────────────────────────

  const modernChrome = (
    <div style={{
      background: theme.primary,
      color: theme.primaryText,
      padding: '14px 20px',
      fontSize: '18px',
      fontWeight: 'bold',
      flexShrink: 0,
      display: 'flex',
      alignItems: 'center',
      gap: '10px',
    }}>
      <span>🌱</span>
      Spring Merch Store
      <ThemeToggle theme={theme} onToggle={toggleTheme} />
    </div>
  )

  // ── Input area ────────────────────────────────────────────────────────────

  const inputArea = isRetro ? (
    <div style={{
      display: 'flex', gap: '6px', padding: '8px 10px',
      background: '#c0c0c0', flexShrink: 0, alignItems: 'center',
      borderTop: '1px solid #ffffff',
    }}>
      <span style={{ fontSize: '11px', color: '#000000', whiteSpace: 'nowrap' }}>Your Message:</span>
      <input
        type="text"
        value={input}
        onChange={e => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Type here and press SEND or hit Enter..."
        disabled={loading}
        style={{
          border: '2px solid', borderColor: '#808080 #ffffff #ffffff #808080',
          background: '#ffffff', flex: 1, padding: '4px 8px',
          fontSize: '13px', outline: 'none', color: '#000000',
          fontFamily: theme.font,
        }}
      />
      <button
        onClick={() => sendMessage()}
        disabled={loading || !input.trim()}
        style={{
          border: '2px solid',
          borderColor: (loading || !input.trim())
            ? '#808080 #ffffff #ffffff #808080'
            : '#ffffff #808080 #808080 #ffffff',
          background: '#c0c0c0', padding: '4px 18px',
          color: loading || !input.trim() ? '#808080' : '#000000',
          cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
          fontSize: '13px', fontWeight: 'bold', fontFamily: theme.font,
        }}
      >
        SEND &gt;&gt;
      </button>
    </div>
  ) : (
    <div style={{
      display: 'flex', gap: '8px', padding: '12px 16px',
      borderTop: `1px solid ${theme.border}`, background: theme.surface, flexShrink: 0,
    }}>
      <input
        type="text"
        value={input}
        onChange={e => setInput(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Ask about Spring merch..."
        disabled={loading}
        style={{
          flex: 1, padding: '10px 14px',
          borderRadius: '8px', border: `1px solid ${theme.border}`,
          fontSize: '14px', outline: 'none', fontFamily: theme.font,
        }}
      />
      <button
        onClick={() => sendMessage()}
        disabled={loading || !input.trim()}
        style={{
          padding: '10px 20px',
          background: theme.primary, color: theme.primaryText,
          border: 'none', borderRadius: '8px',
          cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
          opacity: loading || !input.trim() ? 0.6 : 1,
          fontSize: '14px', fontWeight: 'bold', fontFamily: theme.font,
        }}
      >
        Send
      </button>
    </div>
  )

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div style={{
      display: 'flex', flexDirection: 'column', height: '100vh',
      maxWidth: '800px', margin: '0 auto',
      fontFamily: theme.font,
      background: isRetro ? '#c0c0c0' : theme.surface,
      ...(isRetro ? {
        border: '3px solid', borderColor: '#ffffff #808080 #808080 #ffffff',
      } : {}),
    }}>

      {isRetro ? retroChrome : modernChrome}

      {/* Messages */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: '12px',
        background: theme.surface,
        ...(isRetro ? {
          borderLeft: '2px inset #808080', borderRight: '2px inset #808080',
        } : {}),
      }}>
        {messages.map((msg, i) => (
          <MessageBubble key={i} msg={msg} theme={theme} onAddToOrder={handleAddToOrder} />
        ))}
        {loading && (
          <div style={{
            textAlign: 'left',
            color: isRetro ? theme.success : theme.textMuted,
            fontSize: isRetro ? '12px' : '13px',
            marginBottom: '8px',
            fontStyle: 'italic',
          }}>
            {isRetro
              ? `⏳ Please wait, loading...${tick ? '█' : ' '}`
              : 'Thinking...'}
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {isRetro && <hr style={{ border: 'none', borderTop: '2px inset #808080', flexShrink: 0 }} />}

      {inputArea}

      {/* Status bar (retro only) */}
      {isRetro && (
        <div style={{
          background: '#c0c0c0', borderTop: '1px solid #808080',
          padding: '2px 8px', fontSize: '11px', color: '#000000',
          display: 'flex', gap: '12px', flexShrink: 0,
        }}>
          <span>{loading ? '⏳ Connecting to SpringBot server...' : '✅ Done'}</span>
          <span style={{ marginLeft: 'auto', color: '#808080' }}>
            🔒 Internet zone | Visitors: 001337
          </span>
        </div>
      )}
    </div>
  )
}
