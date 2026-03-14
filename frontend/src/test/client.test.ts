import { describe, it, expect } from 'vitest'
import { ApiError, getErrorMessage } from '../api/client'

describe('getErrorMessage', () => {
  it('returns ApiError message for ApiError instances', () => {
    const error = new ApiError('Unauthorized', 401, { error: 'Unauthorized' })
    expect(getErrorMessage(error, 'fallback')).toBe('Unauthorized')
  })

  it('returns Error message for generic Error instances', () => {
    const error = new Error('Something went wrong')
    expect(getErrorMessage(error, 'fallback')).toBe('Something went wrong')
  })

  it('returns fallback for non-Error values', () => {
    expect(getErrorMessage('string error', 'fallback')).toBe('fallback')
    expect(getErrorMessage(null, 'fallback')).toBe('fallback')
    expect(getErrorMessage(undefined, 'fallback')).toBe('fallback')
    expect(getErrorMessage(42, 'fallback')).toBe('fallback')
  })
})

describe('ApiError', () => {
  it('has correct name, status, and body', () => {
    const error = new ApiError('Not Found', 404, { detail: 'missing' })
    expect(error.name).toBe('ApiError')
    expect(error.status).toBe(404)
    expect(error.body).toEqual({ detail: 'missing' })
    expect(error.message).toBe('Not Found')
    expect(error).toBeInstanceOf(Error)
  })
})
