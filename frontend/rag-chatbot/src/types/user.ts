export interface UserProfile {
  password?: string;
  firstName: string;
  lastName: string;
  gender: string;
  email: string;
  phoneNumber: string;
  birthday: string;
  preferences: string;
  username: string; // Added from UserResponse
}
