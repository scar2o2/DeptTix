export const DEPARTMENT_LABELS = {
  ALL: "All Vel Tech Schools",
  CSE: "School of Computing",
  ECE: "Electrical & Communication",
  EEE: "Electrical & Electronics",
  CIVIL: "Mechanical & Construction",
  MECH: "Mechanical Engineering",
  IT: "Information Technology"
};

export const USER_DEPARTMENTS = ["CSE", "ECE", "EEE", "CIVIL", "MECH", "IT"];
export const EVENT_DEPARTMENTS = ["ALL", ...USER_DEPARTMENTS];

export const getDepartmentLabel = (department) => DEPARTMENT_LABELS[department] || department;
