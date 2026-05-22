import { createContext, useContext, useEffect, useMemo, useState } from "react";
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendEmailVerification,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile
} from "firebase/auth";
import api, { setAuthTokenGetter } from "../services/api";
import { auth, googleProvider } from "../firebase/config";

const AuthContext = createContext(null);

async function resolveAppUser() {
  try {
    const { data } = await api.get("/users/me");
    return data;
  } catch (error) {
    if (error.response?.status !== 404) {
      throw error;
    }
    return null;
  }
}

export function AuthProvider({ children }) {
  const [firebaseUser, setFirebaseUser] = useState(null);
  const [appUser, setAppUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setAuthTokenGetter(async () => {
      if (!auth.currentUser) {
        return null;
      }
      return auth.currentUser.getIdToken();
    });
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      setFirebaseUser(user);

      if (!user) {
        setAppUser(null);
        setLoading(false);
        return;
      }

      if (user.providerData.some((provider) => provider.providerId === "password") && !user.emailVerified) {
        await signOut(auth);
        setAppUser(null);
        setLoading(false);
        return;
      }

      try {
        const nextUser = await resolveAppUser(user);
        setAppUser(nextUser);
      } catch {
        setAppUser(null);
      } finally {
        setLoading(false);
      }
    });

    return unsubscribe;
  }, []);

  const register = async ({ name, email, password }) => {
    const credential = await createUserWithEmailAndPassword(auth, email, password);
    await updateProfile(credential.user, { displayName: name });
    await sendEmailVerification(credential.user);
    await signOut(auth);
  };

  const login = async ({ email, password }) => {
    setLoading(true);
    try {
      const credential = await signInWithEmailAndPassword(auth, email, password);
      if (!credential.user.emailVerified) {
        await signOut(auth);
        throw new Error("Please verify your email before login.");
      }

      const data = await resolveAppUser();
      setFirebaseUser(credential.user);
      setAppUser(data);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const loginWithGoogle = async () => {
    setLoading(true);
    try {
      const credential = await signInWithPopup(auth, googleProvider);
      const data = await resolveAppUser();
      setFirebaseUser(credential.user);
      setAppUser(data);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    await signOut(auth);
    setFirebaseUser(null);
    setAppUser(null);
  };

  const completeProfile = async ({ department }) => {
    setLoading(true);
    try {
      const { data } = await api.post("/users/sync", { department });
      setAppUser(data);
      return data;
    } finally {
      setLoading(false);
    }
  };

  const value = useMemo(() => ({
    firebaseUser,
    appUser,
    loading,
    register,
    login,
    loginWithGoogle,
    completeProfile,
    logout
  }), [firebaseUser, appUser, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
