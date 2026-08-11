import { describe, expect, it, vi } from 'vitest';

import {
  archiveEnvironment,
  createEnvironment,
  fetchEnvironment,
  fetchEnvironments,
  setDefaultEnvironment,
  updateEnvironment,
} from '@/features/environments/api/environmentsApi';

function makeClient() {
  return {
    axios: {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    },
  };
}

describe('environmentsApi', () => {
  it('regression: parses the page envelope from the list endpoint, not a bare array', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        environments: [
          { id: 'env-1', name: 'Home gym', type: 'HOME_GYM', defaultEnvironment: true, active: true },
        ],
        page: 0,
        size: 50,
        totalElements: 1,
      },
    });

    const page = await fetchEnvironments(client as never);
    expect(page.environments).toHaveLength(1);
    expect(page.environments[0]!.name).toBe('Home gym');
  });

  it('sends activeOnly=true by default and honors filters', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: { environments: [], page: 0, size: 50, totalElements: 0 } });
    await fetchEnvironments(client as never, { type: 'HOME_GYM', activeOnly: false, page: 1, size: 10 });
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/environments', {
      params: { type: 'HOME_GYM', equipment: undefined, activeOnly: false, page: 1, size: 10 },
    });
  });

  it('fetches a single environment by id', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { id: 'env-1', name: 'Home gym', type: 'HOME_GYM', defaultEnvironment: false, active: true },
    });
    await fetchEnvironment(client as never, 'env-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/environments/env-1');
  });

  it('omits empty equipment/description/facilityNotes on create', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({
      data: { id: 'env-1', name: 'Track', type: 'TRACK', defaultEnvironment: false, active: true },
    });
    await createEnvironment(client as never, {
      name: 'Track',
      type: 'TRACK',
      availableEquipment: [],
      description: '',
      facilityNotes: '',
    });
    expect(client.axios.post).toHaveBeenCalledWith('/api/v1/training/environments', {
      name: 'Track',
      type: 'TRACK',
      availableEquipment: undefined,
      description: undefined,
      facilityNotes: undefined,
    });
  });

  it('patches with a bare PatchValue body', async () => {
    const client = makeClient();
    client.axios.patch.mockResolvedValue({
      data: { id: 'env-1', name: 'New name', type: 'HOME_GYM', defaultEnvironment: false, active: true },
    });
    await updateEnvironment(client as never, 'env-1', { name: 'New name' });
    expect(client.axios.patch).toHaveBeenCalledWith('/api/v1/training/environments/env-1', {
      name: 'New name',
    });
  });

  it('archives via DELETE', async () => {
    const client = makeClient();
    client.axios.delete.mockResolvedValue({ status: 204 });
    await expect(archiveEnvironment(client as never, 'env-1')).resolves.toBeUndefined();
    expect(client.axios.delete).toHaveBeenCalledWith('/api/v1/training/environments/env-1');
  });

  it('sets an environment as default with an empty POST body', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({
      data: { id: 'env-1', name: 'Home gym', type: 'HOME_GYM', defaultEnvironment: true, active: true },
    });
    const result = await setDefaultEnvironment(client as never, 'env-1');
    expect(client.axios.post).toHaveBeenCalledWith('/api/v1/training/environments/env-1/default', {});
    expect(result.defaultEnvironment).toBe(true);
  });
});
