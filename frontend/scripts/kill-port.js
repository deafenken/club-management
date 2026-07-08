/**
 * Pre-start cleanup — kills zombie processes on the Vite dev-server port.
 *
 * Windows permits 0.0.0.0:5173 + 127.0.0.1:5173 to coexist as separate
 * sockets, and IDEA stopping a Run Configuration kills npm but not the
 * Vite child process.  This script runs before every `npm run dev`.
 *
 * NEW: HTTP health check.  A port can be LISTENING but return 404
 * (zombie Vite from wrong cwd or corrupted state).  We now probe
 * localhost:PORT — only a 200 response counts as "healthy".
 *
 * Rounds:
 *   1. Port free → done.
 *   2. Port busy + HTTP 200 → healthy, leave it alone (exit 0, Vite
 *      strictPort will block the duplicate).
 *   3. Port busy + HTTP ≠ 200 → zombie detected. Kill via netstat +
 *      PowerShell dual-scan → taskkill → Stop-Process → nuclear node.exe.
 *   4. Always exits 0 — strictPort is the final gate.
 */

import { execSync } from 'node:child_process'
import { createServer } from 'node:net'
import { get } from 'node:http'
import { platform } from 'node:os'

const PORT = 5173
const os = platform()
const OWN_PID = process.pid

// ── helpers ──────────────────────────────────────────────────────────

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

/** Check whether `port` is free by attempting to listen on 127.0.0.1. */
function isPortFree(port) {
  return new Promise((resolve) => {
    const s = createServer()
    s.once('error', () => resolve(false))
    s.once('listening', () => { s.close(() => resolve(true)) })
    s.listen(port, '127.0.0.1')
  })
}

/**
 * HTTP GET http://127.0.0.1:PORT/ → true if 200, false otherwise.
 *
 * Uses pure Node.js http.get() — NO external curl dependency.
 * On Windows, `curl` in cmd.exe may be the PowerShell alias
 * (Invoke-WebRequest) or C:\Windows\System32\curl.exe which
 * doesn't support /dev/null, causing false-positive zombie
 * detection.  Node.js http has zero shell dependencies and
 * behaves identically on every platform.
 */
function isViteHealthy(port) {
  return new Promise((resolve) => {
    const req = get(`http://127.0.0.1:${port}/`, { timeout: 5000 }, (res) => {
      res.resume() // discard body — we only care about status code
      resolve(res.statusCode === 200)
    })
    req.on('error', () => resolve(false))
    req.on('timeout', () => { req.destroy(); resolve(false) })
  })
}

// ── detection ────────────────────────────────────────────────────────

function listeningPidsNetstat(port) {
  try {
    const out = execSync(
      `cmd /c "netstat -ano | findstr :${port}"`,
      { encoding: 'utf8', timeout: 5000 }
    )
    const pids = new Set()
    for (const line of out.split('\n').filter(Boolean)) {
      if (!line.includes('LISTENING')) continue
      const parts = line.trim().split(/\s+/)
      const pid = parts[parts.length - 1]
      if (/^\d+$/.test(pid)) pids.add(pid)
    }
    return [...pids]
  } catch {
    return []
  }
}

function listeningPidsPwsh(port) {
  try {
    const out = execSync(
      `powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort ${port} -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess | Sort-Object -Unique"`,
      { encoding: 'utf8', timeout: 10000 }
    )
    return out.trim().split(/\s+/).filter(s => /^\d+$/.test(s))
  } catch {
    return []
  }
}

function allListeningPids(port) {
  return [...new Set([...listeningPidsNetstat(port), ...listeningPidsPwsh(port)])]
}

// ── killing ──────────────────────────────────────────────────────────

function killPid(pid) {
  try {
    execSync(`taskkill /F /PID ${pid}`, { stdio: 'ignore', timeout: 5000 })
    return true
  } catch { return false }
}

function pwshKillPid(pid) {
  try {
    execSync(
      `powershell -NoProfile -Command "Stop-Process -Id ${pid} -Force -ErrorAction SilentlyContinue"`,
      { stdio: 'ignore', timeout: 10000 }
    )
    return true
  } catch { return false }
}

function killAllNodeExceptSelf() {
  try {
    console.log(`[kill-port] ⚠ Last resort: killing all node.exe (except PID ${OWN_PID})...`)
    execSync(
      `powershell -NoProfile -Command "Get-Process -Name node -ErrorAction SilentlyContinue | Where-Object { $_.Id -ne ${OWN_PID} } | Stop-Process -Force -ErrorAction SilentlyContinue"`,
      { stdio: 'ignore', timeout: 10000 }
    )
    return true
  } catch { return false }
}

/** Aggressive multi-round cleanup. Returns true if port is now free. */
async function forceCleanup() {
  // Round 1: netstat + PowerShell dual-scan → taskkill
  let pids = allListeningPids(PORT)
  console.log(`[kill-port] Detected PIDs: ${pids.length > 0 ? pids.join(', ') : '(none)'}`)
  for (const pid of pids) {
    console.log(`[kill-port]   taskkill PID ${pid}: ${killPid(pid) ? 'killed ✓' : 'failed ✗'}`)
  }
  for (let i = 0; i < 6; i++) {
    await sleep(500)
    if (await isPortFree(PORT)) return true
  }

  // Round 2: PowerShell Stop-Process
  console.log('[kill-port] Port still busy. Trying PowerShell Stop-Process...')
  pids = allListeningPids(PORT)
  for (const pid of pids) {
    console.log(`[kill-port]   Stop-Process PID ${pid}: ${pwshKillPid(pid) ? 'done ✓' : 'failed ✗'}`)
  }
  for (let i = 0; i < 6; i++) {
    await sleep(500)
    if (await isPortFree(PORT)) return true
  }

  // Round 3: nuclear option
  killAllNodeExceptSelf()
  await sleep(2000)
  return isPortFree(PORT)
}

// ── main ─────────────────────────────────────────────────────────────

async function main() {
  // Fast path: port free
  if (await isPortFree(PORT)) {
    console.log(`[kill-port] Port ${PORT} is free — nothing to kill.`)
    process.exit(0)
  }

  // Port is busy — health check
  console.log(`[kill-port] Port ${PORT} is occupied. Checking if Vite is healthy...`)
  const healthy = await isViteHealthy(PORT)

  if (healthy) {
    console.log('[kill-port] ✅ Existing Vite is healthy (HTTP 200). Leaving it alone.')
    console.log('[kill-port] Vite strictPort will block the duplicate — stop the old one first if you need a restart.')
    process.exit(0)
  }

  // Zombie detected!
  console.log('[kill-port] ❌ Zombie detected — port occupied but NOT serving 200. Killing it...')
  const cleaned = await forceCleanup()

  if (cleaned) {
    console.log('[kill-port] ✅ Port released. Ready to start Vite.')
  } else {
    console.log('[kill-port] ⚠ Port STILL busy after all attempts.')
    console.log('[kill-port]    Close IDEA, or run: taskkill /F /IM node.exe')
  }
  process.exit(0)
}

main().catch((e) => {
  console.error('[kill-port] Unexpected error:', e.message)
  process.exit(0)
})
