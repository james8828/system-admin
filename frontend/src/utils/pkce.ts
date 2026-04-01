/**
 * PKCE 算法工具函数
 * 用于生成 code_verifier 和 code_challenge
 */

import CryptoJS from 'crypto-js'

/**
 * 生成随机字符串（code_verifier）
 * @returns 64 位十六进制字符串
 */
export function generateCodeVerifier(): string {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return Array.from(array)
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

/**
 * Base64URL 编码工具函数
 * @param base64 Base64 编码的字符串
 * @returns Base64URL 编码的字符串
 */
export function base64UrlEncode(base64: string): string {
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}

/**
 * 计算 code_challenge
 * @param verifier code_verifier 字符串
 * @returns code_challenge 字符串
 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(verifier)
  
  console.log('[Code Challenge 计算过程]')
  console.log('  code_verifier:', verifier)
  console.log('  code_verifier 长度:', verifier.length)
  
  // 使用 crypto-js 库计算 SHA256（支持 HTTP）
  console.log('  ✓ 使用 crypto-js 库计算 SHA256')
  const words = CryptoJS.lib.WordArray.create(data as any)
  const hash = CryptoJS.SHA256(words)
  
  console.log('  ✓ SHA256 计算成功')
  console.log('  SHA256 哈希 (十六进制):', hash.toString(CryptoJS.enc.Hex))
  
  // Base64URL 编码
  const base64 = hash.toString(CryptoJS.enc.Base64)
  const base64url = base64UrlEncode(base64)
  
  console.log('  Base64 编码:', base64)
  console.log('  Base64URL 编码:', base64url)
  console.log('  code_challenge 最终长度:', base64url.length)
  console.log('[✓ 标准 PKCE 算法 - 计算完成]\n')
  
  return base64url
}
