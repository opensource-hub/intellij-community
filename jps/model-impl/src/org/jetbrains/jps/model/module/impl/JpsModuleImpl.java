package org.jetbrains.jps.model.module.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.*;
import org.jetbrains.jps.model.impl.JpsElementChildRoleBase;
import org.jetbrains.jps.model.impl.JpsElementCollectionImpl;
import org.jetbrains.jps.model.impl.JpsNamedCompositeElementBase;
import org.jetbrains.jps.model.impl.JpsUrlListRole;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsLibraryCollection;
import org.jetbrains.jps.model.library.JpsLibraryType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.impl.JpsLibraryCollectionImpl;
import org.jetbrains.jps.model.library.impl.JpsLibraryRole;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.library.sdk.JpsSdkType;
import org.jetbrains.jps.model.library.sdk.JpsSdkReference;
import org.jetbrains.jps.model.module.*;

import java.util.List;

/**
 * @author nik
 */
public class JpsModuleImpl<P extends JpsElement> extends JpsNamedCompositeElementBase<JpsModuleImpl<P>> implements JpsTypedModule<P> {
  private static final JpsUrlListRole CONTENT_ROOTS_ROLE = new JpsUrlListRole("content roots");
  private static final JpsUrlListRole EXCLUDED_ROOTS_ROLE = new JpsUrlListRole("excluded roots");
  public static final JpsElementChildRole<JpsDependenciesListImpl> DEPENDENCIES_LIST_CHILD_ROLE = JpsElementChildRoleBase.create("dependencies");
  private final JpsModuleType<P> myModuleType;
  private final JpsLibraryCollection myLibraryCollection;

  public JpsModuleImpl(JpsModuleType<P> type, @NotNull String name, @NotNull P properties) {
    super(name);
    myModuleType = type;
    myContainer.setChild(myModuleType.getPropertiesRole(), properties);
    myContainer.setChild(CONTENT_ROOTS_ROLE);
    myContainer.setChild(EXCLUDED_ROOTS_ROLE);
    myContainer.setChild(JpsFacetRole.COLLECTION_ROLE);
    myContainer.setChild(DEPENDENCIES_LIST_CHILD_ROLE, new JpsDependenciesListImpl());
    getDependenciesList().addModuleSourceDependency();
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.setChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
    myContainer.setChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE);
    myContainer.setChild(JpsSdkReferencesTableImpl.ROLE);
  }

  private JpsModuleImpl(JpsModuleImpl<P> original) {
    super(original);
    myModuleType = original.myModuleType;
    myLibraryCollection = new JpsLibraryCollectionImpl(myContainer.getChild(JpsLibraryRole.LIBRARIES_COLLECTION_ROLE));
  }

  @NotNull
  @Override
  public JpsModuleImpl<P> createCopy() {
    return new JpsModuleImpl<P>(this);
  }

  @Override
  public JpsElementType<P> getType() {
    return myModuleType;
  }

  @Override
  @NotNull
  public P getProperties() {
    return myContainer.getChild(myModuleType.getPropertiesRole());
  }

  @Override
  public <P extends JpsElement> JpsTypedModule<P> asTyped(@NotNull JpsModuleType<P> type) {
    //noinspection unchecked
    return myModuleType.equals(type) ? (JpsTypedModule<P>)this : null;
  }

  @NotNull
  @Override
  public JpsUrlList getContentRootsList() {
    return myContainer.getChild(CONTENT_ROOTS_ROLE);
  }

  @NotNull
  public JpsUrlList getExcludeRootsList() {
    return myContainer.getChild(EXCLUDED_ROOTS_ROLE);
  }

  @NotNull
  @Override
  public List<JpsModuleSourceRoot> getSourceRoots() {
    return myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE).getElements();
  }

  @NotNull
  @Override
  public <P extends JpsElement, T extends JpsModuleSourceRootType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsModuleSourceRoot addSourceRoot(@NotNull String url, @NotNull T rootType) {
    return addSourceRoot(url, rootType, rootType.createDefaultProperties());
  }

  @NotNull
  @Override
  public <P extends JpsElement> JpsModuleSourceRoot addSourceRoot(@NotNull String url, @NotNull JpsModuleSourceRootType<P> rootType,
                                                                  @NotNull P properties) {
    final JpsModuleSourceRootImpl root = new JpsModuleSourceRootImpl<P>(url, rootType, properties);
    return myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE).addChild(root);
  }

  @Override
  public void removeSourceRoot(@NotNull String url, @NotNull JpsModuleSourceRootType rootType) {
    final JpsElementCollectionImpl<JpsModuleSourceRoot> roots = myContainer.getChild(JpsModuleSourceRootRole.ROOT_COLLECTION_ROLE);
    for (JpsModuleSourceRoot root : roots.getElements()) {
      if (root.getRootType().equals(rootType) && root.getUrl().equals(url)) {
        roots.removeChild(root);
        break;
      }
    }
  }

  @NotNull
  @Override
  public <P extends JpsElement> JpsFacet addFacet(@NotNull String name, @NotNull JpsFacetType<P> type, @NotNull P properties) {
    return myContainer.getChild(JpsFacetRole.COLLECTION_ROLE).addChild(new JpsFacetImpl(type, name, properties));
  }

  @NotNull
  @Override
  public List<JpsFacet> getFacets() {
    return myContainer.getChild(JpsFacetRole.COLLECTION_ROLE).getElements();
  }

  @NotNull
  @Override
  public JpsDependenciesList getDependenciesList() {
    return myContainer.getChild(DEPENDENCIES_LIST_CHILD_ROLE);
  }

  @Override
  @NotNull
  public JpsSdkReferencesTable getSdkReferencesTable() {
    return myContainer.getChild(JpsSdkReferencesTableImpl.ROLE);
  }

  @Override
  public <P extends JpsElement> JpsSdkReference<P> getSdkReference(@NotNull JpsSdkType<P> type) {
    JpsSdkReference<P> sdkReference = getSdkReferencesTable().getSdkReference(type);
    if (sdkReference != null) {
      return sdkReference;
    }
    JpsProject project = getProject();
    if (project != null) {
      return project.getSdkReferencesTable().getSdkReference(type);
    }
    return null;
  }

  @Override
  public <P extends JpsElement> JpsSdk<P> getSdk(@NotNull JpsSdkType<P> type) {
    final JpsSdkReference<P> reference = getSdkReference(type);
    if (reference == null) return null;
    JpsTypedLibrary<JpsSdk<P>> library = reference.resolve();
    return library != null ? library.getProperties() : null;
  }

  @Override
  public void delete() {
    //noinspection unchecked
    ((JpsElementCollection<JpsModule>)myParent).removeChild(this);
  }

  @NotNull
  @Override
  public JpsModuleReference createReference() {
    return new JpsModuleReferenceImpl(getName());
  }

  @NotNull
  @Override
  public <P extends JpsElement, Type extends JpsLibraryType<P> & JpsElementTypeWithDefaultProperties<P>>
  JpsLibrary addModuleLibrary(@NotNull String name, @NotNull Type type) {
    return myLibraryCollection.addLibrary(name, type);
  }

  @Override
  public void addModuleLibrary(final @NotNull JpsLibrary library) {
    myLibraryCollection.addLibrary(library);
  }

  @NotNull
  @Override
  public JpsLibraryCollection getLibraryCollection() {
    return myLibraryCollection;
  }

  @Nullable
  public JpsProject getProject() {
    JpsModel model = getModel();
    return model != null ? model.getProject() : null;
  }

  @NotNull
  @Override
  public JpsModuleType<P> getModuleType() {
    return myModuleType;
  }
}
