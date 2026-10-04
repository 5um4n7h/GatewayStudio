export const msalConfig = {
  auth: {
    clientId: "",
    authority: "https://login.microsoftonline.com/tenandId",
    redirectUri: "http://localhost:5173",
    postLogoutRedirectUri: "http://localhost:5173"
  },
  cache: {
    cacheLocation: "sessionStorage",
    storeAuthStateInCookie: false
  }
};

export const loginRequest = {
  scopes: ["openid", "profile", "User.Read"]
};