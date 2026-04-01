// PKCE Code Challenge 计算验证脚本
// 在浏览器控制台中运行此代码来验证算法

function generateCodeVerifier() {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return Array.from(array)
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}

async function generateCodeChallenge(verifier) {
  const encoder = new TextEncoder()
  const data = encoder.encode(verifier)
  
  console.log('[Code Challenge 计算过程]')
  console.log('  code_verifier:', verifier)
  console.log('  code_verifier 长度:', verifier.length)
  
  if (crypto.subtle) {
    console.log('  使用 crypto.subtle API 计算 SHA256')
    const digest = await crypto.subtle.digest('SHA-256', data)
    
    console.log('  SHA256 哈希 (字节数组):', new Uint8Array(digest))
    console.log('  SHA256 哈希 (十六进制):', Array.from(new Uint8Array(digest))
      .map(b => b.toString(16).padStart(2, '0'))
      .join(''))
    
    // Base64URL 编码
    let binary = ''
    for (let i = 0; i < digest.byteLength; i++) {
      binary += String.fromCharCode(new Uint8Array(digest)[i])
    }
    let base64 = btoa(binary)
    const base64url = base64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '')
    
    console.log('  Base64 编码:', base64)
    console.log('  Base64URL 编码:', base64url)
    console.log('  code_challenge 最终长度:', base64url.length)
    console.log('[计算完成]\n')
    
    return base64url
  } else {
    console.warn('crypto.subtle not available!')
    return null
  }
}

// 测试运行
console.log('\n=== PKCE Code Challenge 测试 ===\n')
const testVerifier = generateCodeVerifier()
generateCodeChallenge(testVerifier).then(challenge => {
  console.log('\n最终结果:')
  console.log('code_verifier:', testVerifier)
  console.log('code_challenge:', challenge)
  console.log('code_challenge_method: S256')
  console.log('===========================\n')
})
