const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'
const TOKEN_KEY = 'kids-game-token'

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: BodyInit | Record<string, unknown> | null
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers)
  const token = localStorage.getItem(TOKEN_KEY)

  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
      body:
        options.body && typeof options.body === 'object' && !(options.body instanceof FormData)
          ? JSON.stringify(options.body)
          : options.body
    })
  } catch (error) {
    const message =
      error instanceof Error && error.message.toLowerCase().includes('failed to fetch')
        ? 'Няма връзка със сървъра. Опитай отново след малко.'
        : 'Възникна проблем с връзката. Опитай отново.'
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('Content-Type') ?? ''
  const payload = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message =
      typeof payload === 'object'
        ? payload.detail || payload.message || payload.error || 'Заявката не беше успешна.'
        : payload || 'Заявката не беше успешна.'
    throw new Error(message)
  }

  return payload as T
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: Record<string, unknown>) =>
    request<T>(path, {
      method: 'POST',
      body: body ?? {}
    }),
  postForm: <T>(path: string, body: FormData) =>
    request<T>(path, {
      method: 'POST',
      body
    }),
  put: <T>(path: string, body?: Record<string, unknown>) =>
    request<T>(path, {
      method: 'PUT',
      body: body ?? {}
    }),
  putForm: <T>(path: string, body: FormData) =>
    request<T>(path, {
      method: 'PUT',
      body
    }),
  delete: <T>(path: string) =>
    request<T>(path, {
      method: 'DELETE'
    })
}

export { TOKEN_KEY }
