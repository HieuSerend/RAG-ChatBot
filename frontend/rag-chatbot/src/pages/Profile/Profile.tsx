import { useEffect, useState } from "react";
import { Edit2, Save, X, ArrowLeft } from "lucide-react";
import { getMyInfo, updateMyInfo } from "../../services/userAPI";
import type { UserProfile } from "../../types/user";

interface ValidationErrors {
  [key: string]: string;
}

export default function Profile() {
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [profile, setProfile] = useState<UserProfile>({
    firstName: "",
    lastName: "",
    gender: "",
    email: "",
    phoneNumber: "",
    birthday: "",
    preferences: "",
  });

  const [editData, setEditData] = useState<UserProfile>(profile);
  const [errors, setErrors] = useState<ValidationErrors>({});

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setIsLoading(true);
        const userData = await getMyInfo();
        const profileData = { ...userData, password: "••••••••" };
        console.log(profileData);
        setProfile(profileData);
        setEditData(profileData);
        setError("");
      } catch (error) {
        console.error("Failed to fetch profile", error);
        setError("Failed to load profile. Please try again later.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchProfile();
  }, []);

  const validateForm = (): boolean => {
    const newErrors: ValidationErrors = {};

    if (!editData.firstName.trim())
      newErrors.firstName = "First name is required";
    if (!editData.lastName.trim()) newErrors.lastName = "Last name is required";
    if (!editData.email.trim()) newErrors.email = "Email is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editData.email))
      newErrors.email = "Invalid email format";
    if (!editData.phoneNumber.trim())
      newErrors.phoneNumber = "Phone number is required";
    if (!editData.birthday) newErrors.birthday = "Birthday is required";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = async () => {
    if (!validateForm()) return;

    setIsSaving(true);
    setMessage("");
    setError("");

    try {
      const dataToUpdate: Partial<UserProfile> = { ...editData };
      if (editData.password === "••••••••") {
        delete dataToUpdate.password;
      }
      const updatedUser = await updateMyInfo(dataToUpdate);
      console.log(dataToUpdate);
      const profileData = { ...updatedUser, password: "••••••••" };
      setProfile(profileData);
      setEditData(profileData);
      setIsEditing(false);
      setMessage("Profile updated successfully!");
    } catch (err) {
      console.error("Failed to update profile", err);
      setError("Failed to update profile. Please try again.");
    } finally {
      setIsSaving(false);
      setTimeout(() => {
        setMessage("");
        setError("");
      }, 5000);
    }
  };

  const handleCancel = () => {
    setEditData(profile);
    setIsEditing(false);
    setErrors({});
  };

  const formatDateForDisplay = (dateString: string) => {
    if (!dateString) return "Not set";
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  };

  const formatDateForInput = (dateString: string) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    return date.toISOString().split("T")[0];
  };

  const handleBackHome = () => {
    window.location.href = "/";
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-2xl mx-auto">
        <div className="mb-6">
          <button
            onClick={handleBackHome}
            className="flex items-center gap-2 text-slate-600 hover:text-indigo-600 transition-colors font-medium px-3 py-2 rounded-lg hover:bg-white/50"
          >
            <ArrowLeft size={20} />
            <span>Back to Home</span>
          </button>
        </div>
        <div className="mb-8">
          <div className="bg-gradient-to-r from-blue-600 to-indigo-600 rounded-lg shadow-lg p-8 text-white">
            <h1 className="text-3xl font-bold text-balance">
              Profile Information
            </h1>
            <p className="text-blue-100 mt-2">Manage your personal details</p>
          </div>
        </div>

        {message && (
          <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg text-green-700 flex items-center justify-between">
            <span>{message}</span>
            <button
              onClick={() => setMessage("")}
              className="text-green-500 hover:text-green-700"
            >
              ×
            </button>
          </div>
        )}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 flex items-center justify-between">
            <span>{error}</span>
            <button
              onClick={() => setError("")}
              className="text-red-500 hover:text-red-700"
            >
              ×
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="bg-white rounded-lg shadow-lg p-6">
            <div className="animate-pulse">
              <div className="h-8 bg-gray-200 rounded w-1/3 mb-6"></div>
              <div className="space-y-6">
                <div className="grid grid-cols-2 gap-6">
                  <div className="h-4 bg-gray-200 rounded w-1/4"></div>
                  <div className="h-4 bg-gray-200 rounded w-1/4"></div>
                </div>
                <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                <div className="h-4 bg-gray-200 rounded w-1/2"></div>
              </div>
            </div>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow-lg overflow-hidden">
            <div className="flex items-center justify-between p-6 border-b border-gray-200">
              <h2 className="text-xl font-semibold text-gray-900">
                Personal Information
              </h2>
              {!isEditing ? (
                <button
                  onClick={() => setIsEditing(true)}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors bg-blue-600 text-white hover:bg-blue-700"
                >
                  <Edit2 size={18} /> Edit
                </button>
              ) : (
                <button
                  onClick={handleCancel}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors bg-gray-200 text-gray-700 hover:bg-gray-300"
                >
                  <X size={18} /> Cancel
                </button>
              )}
            </div>

            <div className="p-6">
              {isEditing ? (
                <form
                  className="space-y-6"
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleSave();
                  }}
                >
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        First Name
                      </label>
                      <input
                        type="text"
                        value={editData.firstName}
                        onChange={(e) =>
                          setEditData({
                            ...editData,
                            firstName: e.target.value,
                          })
                        }
                        className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                          errors.firstName
                            ? "border-red-500"
                            : "border-gray-300"
                        }`}
                        placeholder="Enter first name"
                      />
                      {errors.firstName && (
                        <p className="text-red-500 text-sm mt-1">
                          {errors.firstName}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Last Name
                      </label>
                      <input
                        type="text"
                        value={editData.lastName}
                        onChange={(e) =>
                          setEditData({
                            ...editData,
                            lastName: e.target.value,
                          })
                        }
                        className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                          errors.lastName ? "border-red-500" : "border-gray-300"
                        }`}
                        placeholder="Enter last name"
                      />
                      {errors.lastName && (
                        <p className="text-red-500 text-sm mt-1">
                          {errors.lastName}
                        </p>
                      )}
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Email
                    </label>
                    <input
                      type="email"
                      value={editData.email}
                      onChange={(e) =>
                        setEditData({ ...editData, email: e.target.value })
                      }
                      className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                        errors.email ? "border-red-500" : "border-gray-300"
                      }`}
                      placeholder="Enter email"
                    />
                    {errors.email && (
                      <p className="text-red-500 text-sm mt-1">
                        {errors.email}
                      </p>
                    )}
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    {/* Phone */}
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Phone Number
                      </label>
                      <input
                        type="tel"
                        value={editData.phoneNumber}
                        onChange={(e) =>
                          setEditData({
                            ...editData,
                            phoneNumber: e.target.value,
                          })
                        }
                        className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                          errors.phoneNumber
                            ? "border-red-500"
                            : "border-gray-300"
                        }`}
                        placeholder="Enter phone number"
                      />
                      {errors.phoneNumber && (
                        <p className="text-red-500 text-sm mt-1">
                          {errors.phoneNumber}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Gender
                      </label>
                      <select
                        value={editData.gender}
                        onChange={(e) =>
                          setEditData({
                            ...editData,
                            gender: e.target.value as
                              | "male"
                              | "female"
                              | "other",
                          })
                        }
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="male">Male</option>
                        <option value="female">Female</option>
                        <option value="other">Other</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Birthday
                    </label>
                    <input
                      type="date"
                      value={formatDateForInput(editData.birthday)}
                      onChange={(e) =>
                        setEditData({ ...editData, birthday: e.target.value })
                      }
                      className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                        errors.birthday ? "border-red-500" : "border-gray-300"
                      }`}
                    />
                    {errors.birthday && (
                      <p className="text-red-500 text-sm mt-1">
                        {errors.birthday}
                      </p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Preferences
                    </label>
                    <textarea
                      value={editData.preferences}
                      onChange={(e) =>
                        setEditData({
                          ...editData,
                          preferences: e.target.value,
                        })
                      }
                      rows={4}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                      placeholder="Enter your preferences"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Password
                    </label>
                    <input
                      type="password"
                      value={editData.password}
                      onChange={(e) =>
                        setEditData({ ...editData, password: e.target.value })
                      }
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Enter new password (optional)"
                    />
                  </div>

                  <div className="flex gap-4 pt-4">
                    <button
                      type="submit"
                      disabled={isSaving}
                      className="flex-1 flex items-center justify-center gap-2 bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white font-medium py-2 px-4 rounded-lg transition-colors"
                    >
                      <Save size={18} /> {isSaving ? "Saving..." : "Save"}
                    </button>
                    <button
                      type="button"
                      onClick={handleCancel}
                      className="flex-1 bg-gray-300 hover:bg-gray-400 text-gray-800 font-medium py-2 px-4 rounded-lg transition-colors"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              ) : (
                <div className="space-y-6">
                  {/* Name */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                      <p className="text-sm font-medium text-gray-500">
                        First Name
                      </p>
                      <p className="text-lg text-gray-900 mt-1">
                        {profile.firstName}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">
                        Last Name
                      </p>
                      <p className="text-lg text-gray-900 mt-1">
                        {profile.lastName}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                      <p className="text-sm font-medium text-gray-500">Email</p>
                      <p className="text-lg text-gray-900 mt-1">
                        {profile.email}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">
                        Phone Number
                      </p>
                      <p className="text-lg text-gray-900 mt-1">
                        {profile.phoneNumber}
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                    <div>
                      <p className="text-sm font-medium text-gray-500">
                        Gender
                      </p>
                      <p className="text-lg text-gray-900 mt-1 capitalize">
                        {profile.gender}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-500">
                        Birthday
                      </p>
                      <p className="text-lg text-gray-900 mt-1">
                        {formatDateForDisplay(profile.birthday)}
                      </p>
                    </div>
                  </div>

                  <div>
                    <p className="text-sm font-medium text-gray-500">
                      Preferences
                    </p>
                    <p className="text-lg text-gray-900 mt-1">
                      {profile.preferences || "Not set"}
                    </p>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
