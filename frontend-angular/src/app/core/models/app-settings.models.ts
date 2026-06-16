export interface PublicAppSettings {
  maintenanceMode: boolean;
  chatbotEnabled: boolean;
  importsEnabled: boolean;
  recommendationsEnabled: boolean;
  maxImportFileSizeMb: number;
  welcomeMessage: string;
}

export const DEFAULT_PUBLIC_APP_SETTINGS: PublicAppSettings = {
  maintenanceMode: false,
  chatbotEnabled: true,
  importsEnabled: true,
  recommendationsEnabled: true,
  maxImportFileSizeMb: 10,
  welcomeMessage: 'Bienvenue sur Attijari Compass'
};
