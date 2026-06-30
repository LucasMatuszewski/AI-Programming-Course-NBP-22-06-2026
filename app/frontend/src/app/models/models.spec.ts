import { EQUIPMENT_CATEGORIES } from './models';

describe('EQUIPMENT_CATEGORIES', () => {
  it('should have exactly 10 entries', () => {
    expect(EQUIPMENT_CATEGORIES.length).toBe(10);
  });

  it('every entry has a non-empty value and label', () => {
    for (const cat of EQUIPMENT_CATEGORIES) {
      expect(cat.value.trim()).toBeTruthy();
      expect(cat.label.trim()).toBeTruthy();
    }
  });

  it('includes Smartfony as first entry', () => {
    expect(EQUIPMENT_CATEGORIES[0].label).toBe('Smartfony');
  });

  it('includes Inne as last entry', () => {
    expect(EQUIPMENT_CATEGORIES[9].label).toBe('Inne');
  });

  it('all values are unique', () => {
    const values = EQUIPMENT_CATEGORIES.map((c) => c.value);
    const unique = new Set(values);
    expect(unique.size).toBe(10);
  });
});
