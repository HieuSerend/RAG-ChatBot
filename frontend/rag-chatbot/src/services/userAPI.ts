import api from "./api";
import type { UserProfile } from "../types/user";

export const getMyInfo = async (): Promise<UserProfile> => {
  const response = await api.get("/user/my-info");
  return response.data.data;
};

export const updateMyInfo = async (
  userData: Partial<UserProfile>,
): Promise<UserProfile> => {
  const response = await api.put("/user/update-my-info", userData);
  return response.data.data;
};
