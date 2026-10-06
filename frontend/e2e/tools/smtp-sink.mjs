#!/usr/bin/env node
// Minimal SMTP sink for E2E delivery proof: accepts any message, appends it
// as JSON to the output file, and never requires AUTH. Usage:
//   node smtp-sink.mjs [port] [outFile]

import net from "node:net"
import fs from "node:fs"

const port = Number(process.argv[2] || 1025)
const outFile = process.argv[3] || "/tmp/opencode/smtp-sink-messages.json"

function save(message) {
  let messages = []
  try {
    messages = JSON.parse(fs.readFileSync(outFile, "utf8"))
  } catch {
    messages = []
  }
  messages.push(message)
  fs.writeFileSync(outFile, JSON.stringify(messages, null, 2))
}

function parseMessage(raw, envelope) {
  const normalized = raw.replace(/\r\n/g, "\n")
  const split = normalized.indexOf("\n\n")
  const headerBlock = split >= 0 ? normalized.slice(0, split) : normalized
  const body = split >= 0 ? normalized.slice(split + 2) : ""
  const headers = {}
  for (const line of headerBlock.split("\n")) {
    const idx = line.indexOf(":")
    if (idx > 0) headers[line.slice(0, idx).trim().toLowerCase()] = line.slice(idx + 1).trim()
  }
  return {
    from: envelope.from,
    to: envelope.to,
    subject: headers.subject || "",
    body: body.replace(/^\./gm, ""),
    receivedAt: new Date().toISOString(),
  }
}

const server = net.createServer((socket) => {
  let buffer = ""
  let dataMode = false
  let dataRaw = ""
  const envelope = { from: "", to: [] }

  const write = (line) => socket.write(`${line}\r\n`)

  socket.on("error", () => socket.destroy())
  write("220 elmkusoma-smtp-sink ESMTP")

  socket.on("data", (chunk) => {
    buffer += chunk.toString("utf8")
    let idx
    while (!dataMode && (idx = buffer.indexOf("\n")) >= 0) {
      const line = buffer.slice(0, idx).replace(/\r$/, "")
      buffer = buffer.slice(idx + 1)
      handleCommand(line)
    }
    if (dataMode) {
      const end = buffer.indexOf("\r\n.\r\n")
      if (end >= 0) {
        dataRaw += buffer.slice(0, end)
        buffer = buffer.slice(end + 5)
        dataMode = false
        save(parseMessage(dataRaw, envelope))
        dataRaw = ""
        write("250 2.0.0 Ok: queued")
      }
    }
  })

  function handleCommand(line) {
    const upper = line.toUpperCase()
    if (upper.startsWith("HELO") || upper.startsWith("EHLO")) return write("250 elmkusoma-smtp-sink")
    if (upper.startsWith("MAIL FROM:")) {
      envelope.from = line.slice(line.indexOf(":") + 1).replace(/[<>]/g, "").trim()
      return write("250 2.1.0 Ok")
    }
    if (upper.startsWith("RCPT TO:")) {
      envelope.to.push(line.slice(line.indexOf(":") + 1).replace(/[<>]/g, "").trim())
      return write("250 2.1.5 Ok")
    }
    if (upper === "DATA") {
      dataMode = true
      dataRaw = ""
      return write("354 End data with <CR><LF>.<CR><LF>")
    }
    if (upper === "RSET") {
      envelope.from = ""
      envelope.to = []
      return write("250 2.0.0 Ok")
    }
    if (upper === "NOOP") return write("250 2.0.0 Ok")
    if (upper === "QUIT") {
      write("221 2.0.0 Bye")
      return socket.end()
    }
    return write("502 5.5.2 Command not implemented")
  }
})

server.listen(port, "127.0.0.1", () => {
  process.stderr.write(`smtp-sink listening on 127.0.0.1:${port} -> ${outFile}\n`)
})
