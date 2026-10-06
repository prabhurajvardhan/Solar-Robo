import { AiRequest, AiResponse } from '../../contracts';

export interface AIAdapter {
  respond(request: AiRequest): Promise<AiResponse>;
}
