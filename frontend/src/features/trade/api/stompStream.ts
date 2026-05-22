type StompMessageHandler = (body: string) => void

export function subscribeToTopic(
  destination: string,
  subscriptionId: string,
  onMessage: StompMessageHandler,
) {
  const socket = new WebSocket(getWebSocketUrl())

  socket.addEventListener('open', () => {
    socket.send(toStompFrame('CONNECT', {
      'accept-version': '1.2',
      'heart-beat': '10000,10000',
    }))
  })

  socket.addEventListener('message', (event) => {
    if (typeof event.data !== 'string') {
      return
    }

    parseStompFrames(event.data).forEach((frame) => {
      if (frame.command === 'CONNECTED') {
        socket.send(toStompFrame('SUBSCRIBE', {
          id: subscriptionId,
          destination,
        }))
        return
      }

      if (frame.command === 'MESSAGE' && frame.body) {
        onMessage(frame.body)
      }
    })
  })

  return () => {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(toStompFrame('DISCONNECT', {}))
    }

    if (
      socket.readyState === WebSocket.OPEN ||
      socket.readyState === WebSocket.CONNECTING
    ) {
      socket.close()
    }
  }
}

function getWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws-native`
}

function parseStompFrames(payload: string) {
  return payload
    .split('\0')
    .map((rawFrame) => rawFrame.replaceAll('\r\n', '\n').trim())
    .filter(Boolean)
    .map((rawFrame) => {
      const [headerBlock, ...bodyParts] = rawFrame.split('\n\n')
      const headerLines = headerBlock.split('\n')

      return {
        command: headerLines[0],
        body: bodyParts.join('\n\n'),
      }
    })
}

function toStompFrame(command: string, headers: Record<string, string>) {
  const headerLines = Object.entries(headers).map(
    ([name, value]) => `${name}:${value}`,
  )

  return `${[command, ...headerLines].join('\n')}\n\n\0`
}
