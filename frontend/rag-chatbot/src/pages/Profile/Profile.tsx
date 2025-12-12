import { useEffect, useState } from "react";
import { Edit2, Save, X, ArrowLeft, User, Mail, Phone, Calendar, Shield, AlertCircle } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { getMyInfo, updateMyInfo } from "../../services/userAPI";
import type { UserProfile } from "../../types/user";

interface ValidationErrors {
  [key: string]: string;
}

export default function Profile() {
  const navigate = useNavigate();
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
    username: "", // Added username to profile state
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

    if (!editData.firstName?.trim())
      newErrors.firstName = "First name is required";
    if (!editData.lastName?.trim()) newErrors.lastName = "Last name is required";
    if (!editData.email?.trim()) newErrors.email = "Email is required";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editData.email))
      newErrors.email = "Invalid email format";
    if (!editData.phoneNumber?.trim())
      newErrors.phoneNumber = "Phone number is required";
    if (!editData.birthday) newErrors.birthday = "Birthday is required";

    setErrors(newErrors);

    if (Object.keys(newErrors).length > 0) {
      setError("Please fill in all required fields");
      return false;
    }

    return true;
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
    if (!dateString) return "—"; // Changed from "Not set" to "—" for consistency
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();
    return `${day} /${month}/${year} `;
  };

  const formatDateForInput = (dateString: string) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    return date.toISOString().split("T")[0];
  };

  // handleBackHome is removed as navigate is used directly in the new JSX

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 py-10 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-4xl mx-auto">
        {/* Header Section */}
        <div className="mb-8 flex items-center justify-between">
          <div>
            <button
              onClick={() => navigate('/')}
              className="group flex items-center gap-2 text-slate-400 hover:text-emerald-400 transition-colors mb-4 text-lg font-medium"
            >
              <ArrowLeft size={24} className="group-hover:-translate-x-1 transition-transform" />
              <span>Back to Dashboard</span>
            </button>
            <h1 className="text-3xl font-bold text-slate-100 tracking-tight">Profile Information</h1>
            <p className="text-slate-400 text-base mt-2">Manage your account details and preferences.</p>
          </div>
        </div>

        {/* Feedback Messages */}
        {message && (
          <div className="mb-6 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-400 flex items-center justify-between text-sm animate-fade-in">
            <span className="flex items-center gap-2 font-medium"><Shield size={16} /> {message}</span>
            <button onClick={() => setMessage("")} className="hover:text-emerald-300 transition-colors">
              <X size={16} />
            </button>
          </div>
        )}
        {error && (
          <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 flex items-center justify-between text-sm animate-fade-in">
            <span className="flex items-center gap-2 font-medium"><AlertCircle size={16} /> {error}</span>
            <button onClick={() => setError("")} className="hover:text-red-300 transition-colors">
              <X size={16} />
            </button>
          </div>
        )}

        {/* Content Card */}
        {isLoading ? (
          <div className="bg-slate-800 border border-slate-700 rounded-xl p-8 shadow-xl">
            <div className="animate-pulse space-y-8">
              <div className="h-8 bg-slate-700 rounded w-1/4 mb-4"></div>
              <div className="grid grid-cols-2 gap-8">
                <div className="h-12 bg-slate-700 rounded"></div>
                <div className="h-12 bg-slate-700 rounded"></div>
              </div>
              <div className="h-24 bg-slate-700 rounded w-full"></div>
            </div>
          </div>
        ) : (
          <div className="bg-slate-800 border border-slate-700 rounded-xl shadow-xl overflow-hidden">
            {/* Card Header */}
            <div className="flex items-center justify-between p-6 border-b border-slate-700 bg-slate-800/50">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-slate-700/50 rounded-lg text-emerald-400">
                  <User size={20} />
                </div>
                <h2 className="text-lg font-semibold text-slate-100">Personal Details</h2>
              </div>

              {!isEditing && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-slate-700 text-slate-200 hover:bg-slate-600 hover:text-white border border-slate-600 hover:border-slate-500 transition-all shadow-sm"
                >
                  <Edit2 size={14} /> Edit Profile
                </button>
              )}
            </div>

            <div className="p-6 md:p-8">
              {isEditing ? (
                /* --- EDIT MODE --- */
                <form
                  className="space-y-6"
                  onSubmit={(e) => {
                    e.preventDefault();
                    handleSave();
                  }}
                >
                  {/* Name Fields */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                        First Name
                      </label>
                      <input
                        type="text"
                        value={editData.firstName}
                        onChange={(e) => setEditData({ ...editData, firstName: e.target.value })}
                        className={`w-full px-4 py-2.5 bg-slate-950 border rounded-lg text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors ${errors.firstName ? 'border-red-500/50 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500'
                          }`}
                        placeholder="First Name"
                      />
                      {errors.firstName && <p className="text-red-400 text-xs mt-1.5">{errors.firstName}</p>}
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                        Last Name
                      </label>
                      <input
                        type="text"
                        value={editData.lastName}
                        onChange={(e) => setEditData({ ...editData, lastName: e.target.value })}
                        className={`w-full px-4 py-2.5 bg-slate-950 border rounded-lg text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors ${errors.lastName ? 'border-red-500/50 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500'
                          }`}
                        placeholder="Last Name"
                      />
                      {errors.lastName && <p className="text-red-400 text-xs mt-1.5">{errors.lastName}</p>}
                    </div>
                  </div>

                  {/* Email */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                      Email Address
                    </label>
                    <div className="relative">
                      <Mail size={16} className="absolute left-3.5 top-3.5 text-slate-500" />
                      <input
                        type="email"
                        value={editData.email}
                        onChange={(e) => setEditData({ ...editData, email: e.target.value })}
                        className={`w-full pl-10 pr-4 py-2.5 bg-slate-950 border rounded-lg text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors ${errors.email ? 'border-red-500/50 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500'
                          }`}
                        placeholder="email@company.com"
                      />
                    </div>
                    {errors.email && <p className="text-red-400 text-xs mt-1.5">{errors.email}</p>}
                  </div>

                  {/* Contact & Personal */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                        Phone Number
                      </label>
                      <div className="relative">
                        <Phone size={16} className="absolute left-3.5 top-3.5 text-slate-500" />
                        <input
                          type="tel"
                          value={editData.phoneNumber}
                          onChange={(e) => setEditData({ ...editData, phoneNumber: e.target.value })}
                          className={`w-full pl-10 pr-4 py-2.5 bg-slate-950 border rounded-lg text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors ${errors.phoneNumber ? 'border-red-500/50 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500'
                            }`}
                          placeholder="+1 (555) 000-0000"
                        />
                      </div>
                      {errors.phoneNumber && <p className="text-red-400 text-xs mt-1.5">{errors.phoneNumber}</p>}
                    </div>

                    <div>
                      <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                        Gender
                      </label>
                      <select
                        value={editData.gender}
                        onChange={(e) => setEditData({ ...editData, gender: e.target.value as "male" | "female" | "other" })}
                        className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-lg text-slate-200 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors"
                      >
                        <option value="male">Male</option>
                        <option value="female">Female</option>
                        <option value="other">Other</option>
                      </select>
                    </div>
                  </div>

                  {/* Birthday */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                      Date of Birth
                    </label>
                    <div className="relative">
                      <Calendar size={16} className="absolute left-3.5 top-3.5 text-slate-500" />
                      <input
                        type="date"
                        value={formatDateForInput(editData.birthday)}
                        onChange={(e) => setEditData({ ...editData, birthday: e.target.value })}
                        className={`w-full pl-10 pr-4 py-2.5 bg-slate-950 border rounded-lg text-slate-200 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors [color-scheme:dark] ${errors.birthday ? 'border-red-500/50 focus:border-red-500' : 'border-slate-700 focus:border-emerald-500'
                          }`}
                      />
                    </div>
                    {errors.birthday && <p className="text-red-400 text-xs mt-1.5">{errors.birthday}</p>}
                  </div>

                  {/* Preferences */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                      Investment Preferences
                    </label>
                    <textarea
                      value={editData.preferences}
                      onChange={(e) => setEditData({ ...editData, preferences: e.target.value })}
                      rows={4}
                      className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors resize-none"
                      placeholder="Describe your investment goals and risk tolerance..."
                    />
                  </div>

                  {/* Password */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                      Change Password
                    </label>
                    <input
                      type="password"
                      value={editData.password}
                      onChange={(e) => setEditData({ ...editData, password: e.target.value })}
                      className="w-full px-4 py-2.5 bg-slate-950 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-600 focus:outline-none focus:ring-1 focus:ring-emerald-500 transition-colors"
                      placeholder="Enter new password (optional)"
                    />
                  </div>

                  {/* Action Buttons */}
                  <div className="flex gap-4 pt-6 border-t border-slate-700 mt-6">
                    <button
                      type="button"
                      onClick={handleCancel}
                      className="px-6 py-2.5 rounded-lg text-sm font-medium text-slate-300 bg-slate-700 hover:bg-slate-600 hover:text-white transition-colors"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={isSaving}
                      className="flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-medium text-white bg-emerald-600 hover:bg-emerald-500 hover:shadow-lg hover:shadow-emerald-900/40 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                    >
                      <Save size={16} /> {isSaving ? "Saving..." : "Save Changes"}
                    </button>
                  </div>

                </form>
              ) : (
                /* --- VIEW MODE --- */
                <div className="space-y-8">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-8">
                    {/* Identity Column */}
                    <div className="space-y-6">
                      <div className="flex items-center gap-2 pb-2 border-b border-slate-700/50 mb-4">
                        <Shield size={16} className="text-emerald-500" />
                        <h3 className="text-sm font-semibold text-slate-200">Identity Details</h3>
                      </div>

                      <div className="grid grid-cols-2 gap-6">
                        <div>
                          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">First Name</p>
                          <p className="text-slate-200 text-sm font-medium">{profile.firstName || '—'}</p>
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Last Name</p>
                          <p className="text-slate-200 text-sm font-medium">{profile.lastName || '—'}</p>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-6">
                        <div>
                          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Gender</p>
                          <p className="text-slate-200 text-sm font-medium capitalize">{profile.gender || '—'}</p>
                        </div>
                        <div>
                          <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Birthday</p>
                          <p className="text-slate-200 text-sm font-medium">{formatDateForDisplay(profile.birthday)}</p>
                        </div>
                      </div>
                    </div>

                    {/* Contact Column */}
                    <div className="space-y-6">
                      <div className="flex items-center gap-2 pb-2 border-b border-slate-700/50 mb-4">
                        <Mail size={16} className="text-emerald-500" />
                        <h3 className="text-sm font-semibold text-slate-200">Contact Info</h3>
                      </div>

                      <div>
                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Email Address</p>
                        <p className="text-slate-200 text-sm font-medium font-mono">{profile.email || '—'}</p>
                      </div>
                      <div>
                        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">Phone Number</p>
                        <p className="text-slate-200 text-sm font-medium font-mono">{profile.phoneNumber || '—'}</p>
                      </div>
                    </div>
                  </div>

                  {/* Preferences Section */}
                  <div className="pt-6 border-t border-slate-700/50">
                    <div className="flex items-center gap-2 mb-4">
                      <User size={16} className="text-emerald-500" />
                      <h3 className="text-sm font-semibold text-slate-200">Investment Profile</h3>
                    </div>
                    <div className="bg-slate-900/50 border border-slate-700/50 rounded-lg p-5">
                      <p className="text-slate-300 text-sm leading-relaxed whitespace-pre-wrap">
                        {profile.preferences || "No detailed preference profile available."}
                      </p>
                    </div>
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
